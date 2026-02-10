import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useLanguage } from '../contexts/LanguageContext';
import { supabase } from '../lib/supabase';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { cn } from '@/lib/utils';
import { Loader2 } from 'lucide-react';

export const ProfileModal = ({ user: profileUser, trigger, open, onOpenChange }) => {
  const { user: authUser } = useAuth();
  const { t, language, setLanguage } = useLanguage();
  const [uploading, setUploading] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [passwordMessage, setPasswordMessage] = useState('');
  
  const isMe = authUser?.id === profileUser?.id;

  const handleAvatarUpload = async (event) => {
    try {
      setUploading(true);
      if (!event.target.files || event.target.files.length === 0) {
        throw new Error('You must select an image to upload.');
      }

      const file = event.target.files[0];
      const fileExt = file.name.split('.').pop();
      const fileName = `${authUser.id}/${Math.random()}.${fileExt}`;
      const filePath = `${fileName}`;

      let { error: uploadError } = await supabase.storage
        .from('avatars')
        .upload(filePath, file);

      if (uploadError) {
        throw uploadError;
      }

      const { data: { publicUrl } } = supabase.storage
        .from('avatars')
        .getPublicUrl(filePath);

      const { error: updateError } = await supabase
        .from('profiles')
        .update({ avatar_url: publicUrl })
        .eq('id', authUser.id);

      if (updateError) {
        throw updateError;
      }
      
      // Update local state or trigger refetch if needed
      // Ideally context or SWR/Query handles this, but for now we rely on DB update and subscription in ChatWindow
      
    } catch (error) {
      console.error(error);
      alert(error.message);
    } finally {
      setUploading(false);
    }
  };

  const handlePasswordReset = async () => {
    try {
      setPasswordLoading(true);
      const { error } = await supabase.auth.resetPasswordForEmail(profileUser.email, {
        redirectTo: window.location.origin + '/reset-password',
      });
      if (error) throw error;
      setPasswordMessage(t('emailSent'));
    } catch (error) {
      console.error(error);
      setPasswordMessage(t('error') + ': ' + error.message);
    } finally {
      setPasswordLoading(false);
    }
  };

  const handleLanguageChange = async (value) => {
    setLanguage(value);
    if (isMe) {
        await supabase.from('profiles').update({ language: value }).eq('id', authUser.id);
    }
  };

  if (!profileUser) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      {trigger && <DialogTrigger asChild>{trigger}</DialogTrigger>}
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>{t('profile')}</DialogTitle>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="flex flex-col items-center gap-4">
            <Avatar className="h-24 w-24">
              <AvatarImage src={profileUser.avatar_url} />
              <AvatarFallback className="text-2xl">{profileUser.username?.[0]?.toUpperCase()}</AvatarFallback>
            </Avatar>
            {isMe && (
              <div className="flex items-center gap-2">
                <Input 
                  type="file" 
                  accept="image/*" 
                  onChange={handleAvatarUpload} 
                  disabled={uploading}
                  className="w-full max-w-xs text-xs"
                />
                {uploading && <Loader2 className="h-4 w-4 animate-spin" />}
              </div>
            )}
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">{t('username')}</Label>
            <div className="col-span-3 font-medium">{profileUser.username}</div>
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">{t('fullName')}</Label>
            <div className="col-span-3 font-medium">{profileUser.full_name}</div>
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">{t('status')}</Label>
            <div className={cn("col-span-3 flex items-center gap-2", profileUser.status === 'online' ? "text-green-500" : "text-muted-foreground")}>
              <span className={cn("h-2 w-2 rounded-full", profileUser.status === 'online' ? "bg-green-500" : "bg-gray-400")} />
              {profileUser.status === 'online' ? t('online') : t('offline')}
            </div>
          </div>

          {isMe && (
            <>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label className="text-right">{t('language')}</Label>
                <Select value={language} onValueChange={handleLanguageChange}>
                  <SelectTrigger className="col-span-3">
                    <SelectValue placeholder={t('language')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="en">{t('english')}</SelectItem>
                    <SelectItem value="es">{t('spanish')}</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label className="text-right">{t('password')}</Label>
                <div className="col-span-3 flex flex-col gap-2">
                    <Button variant="outline" size="sm" onClick={handlePasswordReset} disabled={passwordLoading}>
                        {passwordLoading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                        {t('changePassword')}
                    </Button>
                    {passwordMessage && <span className="text-xs text-muted-foreground">{passwordMessage}</span>}
                </div>
              </div>
            </>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
};
