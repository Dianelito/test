import React, { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { supabase } from '../../lib/supabase';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Loader2 } from 'lucide-react';
import { useLanguage } from '../../contexts/LanguageContext';
import { toast } from 'sonner';

export const AccountSettings = () => {
  const { user } = useAuth();
  const { t } = useLanguage();
  const [loading, setLoading] = useState(false);
  const [passwords, setPasswords] = useState({
    newPassword: '',
    confirmPassword: ''
  });

  const handleUpdatePassword = async (e) => {
    e.preventDefault();
    if (passwords.newPassword !== passwords.confirmPassword) {
      toast.error(t('passwordsDoNotMatch') || 'Passwords do not match');
      return;
    }
    
    if (passwords.newPassword.length < 6) {
      toast.error(t('passwordTooShort') || 'Password must be at least 6 characters');
      return;
    }

    setLoading(true);
    try {
      const { error } = await supabase.auth.updateUser({ 
        password: passwords.newPassword 
      });
      
      if (error) throw error;
      toast.success(t('passwordUpdated') || 'Password updated successfully');
      setPasswords({ newPassword: '', confirmPassword: '' });
    } catch (error) {
      console.error('Error updating password:', error);
      toast.error(error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>{t('changePassword') || 'Change Password'}</CardTitle>
          <CardDescription>
            {t('changePasswordDesc') || 'Update your password to keep your account secure.'}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleUpdatePassword} className="space-y-4 max-w-md">
            <div className="grid gap-2">
              <Label htmlFor="newPassword">{t('newPassword') || 'New Password'}</Label>
              <Input 
                id="newPassword" 
                type="password"
                value={passwords.newPassword} 
                onChange={(e) => setPasswords({...passwords, newPassword: e.target.value})} 
                placeholder="••••••••"
                minLength={6}
                required
              />
            </div>
            
            <div className="grid gap-2">
              <Label htmlFor="confirmPassword">{t('confirmPassword') || 'Confirm Password'}</Label>
              <Input 
                id="confirmPassword" 
                type="password"
                value={passwords.confirmPassword} 
                onChange={(e) => setPasswords({...passwords, confirmPassword: e.target.value})} 
                placeholder="••••••••"
                minLength={6}
                required
              />
            </div>

            <Button type="submit" disabled={loading}>
              {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {t('updatePassword') || 'Update Password'}
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t('accountDetails') || 'Account Details'}</CardTitle>
          <CardDescription>
            {t('accountDetailsDesc') || 'Your account information.'}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-1">
            <Label>{t('email') || 'Email'}</Label>
            <div className="text-sm text-muted-foreground font-mono bg-muted p-2 rounded-md">
              {user?.email}
            </div>
          </div>
          <div className="space-y-1">
            <Label>{t('userId') || 'User ID'}</Label>
            <div className="text-xs text-muted-foreground font-mono bg-muted p-2 rounded-md overflow-x-auto">
              {user?.id}
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
