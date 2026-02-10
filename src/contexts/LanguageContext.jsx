import React, { createContext, useContext, useState } from 'react';

const LanguageContext = createContext();

export const useLanguage = () => useContext(LanguageContext);

const translations = {
  es: {
    login: "Iniciar Sesión",
    signup: "Registrarse",
    email: "Correo electrónico",
    password: "Contraseña",
    fullName: "Nombre completo",
    username: "Nombre de usuario",
    haveAccount: "¿Ya tienes cuenta?",
    noAccount: "¿No tienes cuenta?",
    send: "Enviar",
    search: "Buscar usuarios...",
    online: "En línea",
    offline: "Desconectado",
    settings: "Configuración",
    theme: "Tema",
    language: "Idioma",
    logout: "Cerrar Sesión",
    messages: "Mensajes",
    startChat: "Selecciona un usuario para chatear",
    typeMessage: "Escribe un mensaje...",
    usernameTaken: "El nombre de usuario ya está en uso",
    darkMode: "Modo Oscuro",
    lightMode: "Modo Claro",
    english: "Inglés",
    spanish: "Español",
    welcome: "Bienvenido a ChatIt",
    subtitle: "Conecta con amigos y colegas en tiempo real.",
    loggingIn: "Iniciando sesión...",
    signingUp: "Registrando...",
    error: "Error",
    success: "Éxito",
  },
  en: {
    username: "Username",
    fullName: "Full Name",
    password: "Password",
    email: "Email",
    login: "Log In",
    signup: "Sign Up",
    loggingIn: "Logging in...",
    signingUp: "Signing up...",
    welcome: "Welcome to ChatIt",
    subtitle: "Connect with friends and colleagues in real-time.",
    search: "Search users...",
    startChat: "Select a user to start chatting",
    typeMessage: "Type a message...",
    send: "Send",
    online: "Online",
    offline: "Offline",
    profile: "Profile",
    settings: "Settings",
    changePassword: "Change Password",
    changeAvatar: "Change Avatar",
    logout: "Log Out",
    language: "Language",
    theme: "Theme",
    save: "Save",
    cancel: "Cancel",
    close: "Close",
    uploading: "Uploading...",
    passwordUpdated: "Password updated successfully",
    emailSent: "Password reset email sent",
    error: "Error",
    success: "Success",
    darkMode: "Dark Mode",
    lightMode: "Light Mode",
    spanish: "Spanish",
    english: "English",
    myProfile: "My Profile"
  }
};

export const LanguageProvider = ({ children }) => {
  const [language, setLanguage] = useState('en');

  const t = (key) => {
    return translations[language][key] || key;
  };

  const value = {
    language,
    setLanguage,
    t
  };

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
};
