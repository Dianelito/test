import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { supabase } from '../../lib/supabase';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Loader2, Upload, Camera } from "lucide-react";
import { toast } from "sonner";
import { useLanguage } from '../../contexts/LanguageContext';

export const ProfileSettings = () => {
  const { user } = useAuth();
  const { t } = useLanguage();
  const [profile, setProfile] = useState({
    full_name: '',
    username: '',
    bio: '',
    status: '',
    avatar_url: '',
    banner_url: ''
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [uploadingBanner, setUploadingBanner] = useState(false);

  useEffect(() => {
    if (user) {
      fetchProfile();
    }
  }, [user]);

  const fetchProfile = async () => {
    try {
      setLoading(true);
      const { data, error } = await supabase
        .from('profiles')
        .select('*')
        .eq('id', user.id)
        .single();

      if (error) throw error;

      if (data) {
        setProfile({
          full_name: data.full_name || '',
          username: data.username || '',
          bio: data.bio || '',
          status: data.status || '',
          avatar_url: data.avatar_url || '',
          banner_url: data.banner_url || ''
        });
      }
    } catch (error) {
      console.error('Error fetching profile:', error);
      toast.error('Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    try {
      setSaving(true);

      const updates = {
        id: user.id,
        full_name: profile.full_name,
        bio: profile.bio,
        status: profile.status,
        updated_at: new Date().toISOString(),
      };

      const { error } = await supabase.from('profiles').upsert(updates);

      if (error) throw error;
      toast.success('Profile updated successfully');
    } catch (error) {
      console.error('Error updating profile:', error);
      toast.error('Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  const uploadImage = async (event, bucket, field) => {
    try {
      const file = event.target.files?.[0];
      if (!file) return;

      if (field === 'avatar_url') setUploadingAvatar(true);
      else setUploadingBanner(true);
      
      const fileExt = file.name.split('.').pop();
      const fileName = `${user.id}/${Math.random()}.${fileExt}`;
      const filePath = `${fileName}`;

      const { error: uploadError } = await supabase.storage
        .from(bucket)
        .upload(filePath, file);

      if (uploadError) throw uploadError;

      const { data: urlData } = supabase.storage
        .from(bucket)
        .getPublicUrl(filePath);

      const publicUrl = urlData.publicUrl;

      // Update profile immediately with new URL
      const { error: updateError } = await supabase
        .from('profiles')
        .update({ [field]: publicUrl })
        .eq('id', user.id);

      if (updateError) throw updateError;

      setProfile(prev => ({ ...prev, [field]: publicUrl }));
      toast.success('Image uploaded successfully');

    } catch (error) {
      console.error('Error uploading image:', error);
      toast.error('Failed to upload image');
    } finally {
      if (field === 'avatar_url') setUploadingAvatar(false);
      else setUploadingBanner(false);
    }
  };

  if (loading) {
    return <div className="flex justify-center p-8"><Loader2 className="h-8 w-8 animate-spin" /></div>;
  }

  return (
    <div className="space-y-8">
      {/* Banner Section */}
      <div className="relative h-48 rounded-lg overflow-hidden bg-muted group">
        {profile.banner_url ? (
          <img src={profile.banner_url} alt="Profile Banner" className="w-full h-full object-cover" />
        ) : (
          <div className="w-full h-full bg-gradient-to-r from-blue-400 to-purple-500" />
        )}
        <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
          <Label htmlFor="banner-upload" className="cursor-pointer">
            <div className="bg-background/80 hover:bg-background text-foreground px-4 py-2 rounded-md flex items-center gap-2 transition-colors">
              {uploadingBanner ? <Loader2 className="h-4 w-4 animate-spin" /> : <Camera className="h-4 w-4" />}
              <span>{t('changeBanner') || 'Change Banner'}</span>
            </div>
          </Label>
          <Input 
            id="banner-upload" 
            type="file" 
            accept="image/*" 
            className="hidden" 
            onChange={(e) => uploadImage(e, 'avatars', 'banner_url')}
            disabled={uploadingBanner}
          />
        </div>
      </div>

      {/* Avatar Section (overlapping banner) */}
      <div className="relative -mt-16 ml-6 mb-6 inline-block group">
        <Avatar className="h-32 w-32 border-4 border-background shadow-lg">
          <AvatarImage src={profile.avatar_url} alt={profile.full_name} />
          <AvatarFallback className="text-4xl">{profile.full_name?.[0] || user?.email?.[0]?.toUpperCase()}</AvatarFallback>
        </Avatar>
        <Label htmlFor="avatar-upload" className="absolute bottom-0 right-0 cursor-pointer">
          <div className="bg-primary text-primary-foreground p-2 rounded-full shadow-lg hover:bg-primary/90 transition-colors">
            {uploadingAvatar ? <Loader2 className="h-4 w-4 animate-spin" /> : <Camera className="h-4 w-4" />}
          </div>
        </Label>
        <Input 
          id="avatar-upload" 
          type="file" 
          accept="image/*" 
          className="hidden" 
          onChange={(e) => uploadImage(e, 'avatars', 'avatar_url')}
          disabled={uploadingAvatar}
        />
      </div>

      <form onSubmit={handleUpdate} className="space-y-6">
        <div className="grid gap-6 md:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="full_name">{t('fullName') || 'Full Name'}</Label>
            <Input
              id="full_name"
              value={profile.full_name}
              onChange={(e) => setProfile({ ...profile, full_name: e.target.value })}
              placeholder={t('enterFullName') || 'Your full name'}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="username">{t('username') || 'Username'} ({t('readOnly') || 'Read Only'})</Label>
            <Input
              id="username"
              value={profile.username}
              disabled
              className="bg-muted"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="status">{t('status') || 'Status'}</Label>
            <Input
              id="status"
              value={profile.status}
              onChange={(e) => setProfile({ ...profile, status: e.target.value })}
              placeholder={t('enterStatus') || 'Online, Busy, Away...'}
            />
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="bio">{t('bio') || 'Bio'}</Label>
          <Textarea
            id="bio"
            value={profile.bio}
            onChange={(e) => setProfile({ ...profile, bio: e.target.value })}
            placeholder={t('enterBio') || 'Tell us a little bit about yourself'}
            rows={4}
          />
        </div>

        <div className="flex justify-end">
          <Button type="submit" disabled={saving}>
            {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {t('saveChanges') || 'Save Changes'}
          </Button>
        </div>
      </form>
    </div>
  );
};
