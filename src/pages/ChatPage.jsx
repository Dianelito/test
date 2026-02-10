import React, { useState, useEffect } from 'react';
import { UserList } from '../components/UserList';
import { ChatWindow } from '../components/ChatWindow';
import { ThemeToggle } from '../components/ThemeToggle';
import { LanguageToggle } from '../components/LanguageToggle';
import { useAuth } from '../contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { LogOut, Menu, User } from 'lucide-react';
import { useLanguage } from '../contexts/LanguageContext';
import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet';
import { ProfileModal } from '@/components/ProfileModal';
import { supabase } from '@/lib/supabase';

const ChatPage = () => {
  const { user, signOut } = useAuth();
  const { t } = useLanguage();
  const [selectedUser, setSelectedUser] = useState(null);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [myProfileOpen, setMyProfileOpen] = useState(false);
  const [myProfile, setMyProfile] = useState(null);

  useEffect(() => {
    if (user) {
      const fetchProfile = async () => {
        const { data } = await supabase.from('profiles').select('*').eq('id', user.id).single();
        if (data) setMyProfile(data);
      };
      fetchProfile();
    }
  }, [user, myProfileOpen]); // Refetch when modal opens/closes to ensure fresh data

  const handleSelectUser = (user) => {
    setSelectedUser(user);
    setIsMobileMenuOpen(false);
  };

  return (
    <div className="flex h-screen bg-background text-foreground overflow-hidden">
      <ProfileModal 
        user={myProfile} 
        open={myProfileOpen} 
        onOpenChange={setMyProfileOpen} 
      />

      {/* Sidebar for Desktop */}
      <div className="hidden md:flex md:w-80 flex-col border-r h-full">
        <div className="p-4 border-b flex justify-between items-center bg-muted/30 shrink-0">
          <div className="font-bold text-lg">ChatIt</div>
        </div>
        <div className="flex-1 overflow-hidden flex flex-col">
            <UserList onSelectUser={handleSelectUser} selectedUserId={selectedUser?.id} />
        </div>
        <div className="p-4 border-t bg-muted/30 flex flex-col gap-3 shrink-0">
            <Button variant="outline" className="w-full justify-start gap-2" onClick={() => setMyProfileOpen(true)}>
                <User className="h-4 w-4" />
                {t('myProfile')}
            </Button>
            
            <div className="flex justify-between items-center">
                <div className="flex gap-1">
                    <ThemeToggle />
                    <LanguageToggle />
                </div>
                <Button variant="ghost" size="icon" onClick={() => signOut()} title={t('logout')}>
                    <LogOut className="h-4 w-4" />
                </Button>
            </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col h-full w-full relative">
        {/* Mobile Header */}
        <div className="md:hidden flex items-center justify-between p-4 border-b bg-background z-20">
          <Sheet open={isMobileMenuOpen} onOpenChange={setIsMobileMenuOpen}>
            <SheetTrigger asChild>
              <Button variant="ghost" size="icon">
                <Menu className="h-6 w-6" />
              </Button>
            </SheetTrigger>
            <SheetContent side="left" className="p-0 w-80 flex flex-col h-full">
              <div className="p-4 border-b flex justify-between items-center bg-muted/30 shrink-0">
                <div className="font-bold text-lg">ChatIt</div>
              </div>
              <div className="flex-1 overflow-y-auto">
                 <UserList onSelectUser={handleSelectUser} selectedUserId={selectedUser?.id} />
              </div>
              <div className="p-4 border-t bg-muted/30 flex flex-col gap-3 shrink-0">
                <Button variant="outline" className="w-full justify-start gap-2" onClick={() => setMyProfileOpen(true)}>
                    <User className="h-4 w-4" />
                    {t('myProfile')}
                </Button>
                <div className="flex justify-between items-center">
                    <div className="flex gap-1">
                        <ThemeToggle />
                        <LanguageToggle />
                    </div>
                    <Button variant="ghost" size="icon" onClick={() => signOut()}>
                        <LogOut className="h-4 w-4" />
                    </Button>
                </div>
              </div>
            </SheetContent>
          </Sheet>
          <div className="font-bold">ChatIt</div>
          <div className="w-8" /> {/* Spacer */}
        </div>

        <ChatWindow selectedUser={selectedUser} />
      </div>
    </div>
  );
};

export default ChatPage;
