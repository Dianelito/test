import React, { createContext, useContext, useState, useEffect } from 'react';
import { supabase } from '../lib/supabase';

const AuthContext = createContext();

export const useAuth = () => {
  return useContext(AuthContext);
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check current session
    const getSession = async () => {
      const { data: { session }, error } = await supabase.auth.getSession();
      if (error) {
        console.error('Error getting session:', error);
      }
      setUser(session?.user ?? null);
      setLoading(false);
    };

    getSession();

    // Listen for auth changes
    const { data: { subscription } } = supabase.auth.onAuthStateChange(async (_event, session) => {
      setUser(session?.user ?? null);
      setLoading(false);
    });

    return () => {
      subscription.unsubscribe();
    };
  }, []);

  const signUp = async ({ email, password, fullName, username, language = 'en' }) => {
    try {
      // 1. Check if username exists (client side fast check)
      const { data: existingUser, error: checkError } = await supabase
        .from('profiles')
        .select('username')
        .eq('username', username)
        .maybeSingle();

      if (checkError) throw checkError;
      if (existingUser) {
        return { error: { message: 'Username already taken' } };
      }

      // 2. Call Edge Function to create user (Bypassing Supabase Email Sending)
      // This uses the Admin API to create a confirmed user directly, avoiding email rate limits
      const { data: funcData, error: funcError } = await supabase.functions.invoke('create-user', {
        body: {
          email,
          password,
          fullName,
          username,
          language
        }
      });

      if (funcError) {
        console.error('Error creating user via edge function:', funcError);
        // Try to parse the error message from the response if available
        try {
          // If the error is an object with a context/response
          if (funcError.context && funcError.context.json) {
            const errorBody = await funcError.context.json();
            if (errorBody.error) {
              return { error: { message: errorBody.error.message || errorBody.error } };
            }
          }
        } catch (e) {
          // Ignore parsing errors
        }
        return { error: funcError };
      }
      
      // Check for application-level error in the response body (if 200 OK but logical error)
      if (funcData?.error) {
        return { error: { message: funcData.error.message || funcData.error } };
      }

      // 3. Sign in immediately (User is already confirmed by the function)
      const { data: signInData, error: signInError } = await supabase.auth.signInWithPassword({
        email,
        password
      });
      
      if (signInError) {
        console.error('Error signing in after signup:', signInError);
        return { error: signInError };
      }
      
      return { data: signInData, error: null };
    } catch (error) {
      console.error('SignUp Error:', error);
      return { error };
    }
  };

  const signIn = async ({ email, password }) => {
    const { data, error } = await supabase.auth.signInWithPassword({
      email,
      password,
    });
    return { data, error };
  };

  const signOut = async () => {
    try {
      // Update status to offline before signing out
      if (user) {
        await supabase
          .from('profiles')
          .update({ status: 'offline', last_seen: new Date().toISOString() })
          .eq('id', user.id);
      }
    } catch (error) {
      console.error('Error updating status on signout:', error);
    }
    
    const { error } = await supabase.auth.signOut();
    return { error };
  };

  const value = {
    signUp,
    signIn,
    signOut,
    user,
    loading
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
