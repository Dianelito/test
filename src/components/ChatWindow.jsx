import React, { useEffect, useState, useRef } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from '../contexts/AuthContext';
import { useLanguage } from '../contexts/LanguageContext';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Send, UserX, MessageSquare } from 'lucide-react';
import { cn } from '@/lib/utils';
import { format } from 'date-fns';
import { ProfileModal } from './ProfileModal';

export const ChatWindow = ({ selectedUser }) => {
  const { user } = useAuth();
  const { t } = useLanguage();
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [friendshipStatus, setFriendshipStatus] = useState(null);
  const [isProfileOpen, setProfileOpen] = useState(false);
  const scrollRef = useRef(null);

  useEffect(() => {
    if (!selectedUser || !user) return;

    const checkFriendship = async () => {
      const { data } = await supabase
        .from('friendships')
        .select('status')
        .or(`and(user_id.eq.${user.id},friend_id.eq.${selectedUser.id}),and(user_id.eq.${selectedUser.id},friend_id.eq.${user.id})`)
        .maybeSingle();

      setFriendshipStatus(data?.status || null);
    };

    const fetchMessages = async () => {
      const { data, error } = await supabase
        .from('messages')
        .select('*')
        .or(`and(sender_id.eq.${user.id},receiver_id.eq.${selectedUser.id}),and(sender_id.eq.${selectedUser.id},receiver_id.eq.${user.id})`)
        .order('created_at', { ascending: true });
      
      if (data) setMessages(data);
    };

    checkFriendship();
    fetchMessages();

    // Subscribe to new messages for this chat
    const channel = supabase
      .channel(`chat:${user.id}:${selectedUser.id}`)
      .on('postgres_changes', { 
        event: 'INSERT', 
        schema: 'public', 
        table: 'messages'
      }, (payload) => {
        // Check if message belongs to this conversation
        const isRelated = 
          (payload.new.sender_id === user.id && payload.new.receiver_id === selectedUser.id) ||
          (payload.new.sender_id === selectedUser.id && payload.new.receiver_id === user.id);

        if (isRelated) {
          setMessages(prev => {
             // Avoid duplicates if any
             if (prev.some(m => m.id === payload.new.id)) return prev;
             return [...prev, payload.new];
          });
        }
      })
      .subscribe();

    return () => {
      supabase.removeChannel(channel);
    };
  }, [selectedUser, user]);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  const handleSend = async (e) => {
    e.preventDefault();
    if (!newMessage.trim() || !user || !selectedUser) return;

    const { error } = await supabase.from('messages').insert({
      sender_id: user.id,
      receiver_id: selectedUser.id,
      content: newMessage,
    });

    if (error) {
      console.error('Error sending message:', error);
    } else {
      setNewMessage('');
    }
  };

  if (!selectedUser) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-muted-foreground p-8 text-center bg-muted/5">
        <div className="bg-background p-4 rounded-full mb-4 shadow-sm">
          <MessageSquare className="h-8 w-8 text-primary" />
        </div>
        <div className="text-xl font-medium mb-2">{t('startChat') || 'Select a friend to chat'}</div>
        <p className="text-sm opacity-70 max-w-xs">{t('search') || 'Choose a friend from the list or add new friends to start messaging.'}</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full w-full">
      <div 
        className="flex items-center gap-3 p-4 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 sticky top-0 z-10 shadow-sm cursor-pointer hover:bg-accent/50 transition-colors"
        onClick={() => setProfileOpen(true)}
      >
        <Avatar>
          <AvatarImage src={selectedUser.avatar_url} />
          <AvatarFallback>{selectedUser.username ? selectedUser.username[0].toUpperCase() : '?'}</AvatarFallback>
        </Avatar>
        <div>
          <div className="font-semibold">{selectedUser.full_name || selectedUser.username}</div>
          <div className="text-xs text-muted-foreground flex items-center gap-1">
            <span className={cn("w-2 h-2 rounded-full", selectedUser.status === 'online' ? "bg-green-500" : "bg-gray-400")} />
            {selectedUser.status === 'online' ? t('online') : t('offline')}
          </div>
        </div>
      </div>
      
      <ProfileModal 
        user={selectedUser} 
        open={isProfileOpen} 
        onOpenChange={setProfileOpen} 
      />

      <ScrollArea className="flex-1 p-4 bg-muted/5">
        <div className="flex flex-col gap-4 min-h-0">
          {messages.map((msg, index) => {
            const isMe = msg.sender_id === user.id;
            const showAvatar = !isMe && (index === 0 || messages[index - 1].sender_id !== msg.sender_id);
            
            return (
              <div
                key={msg.id}
                className={cn(
                  "flex gap-2 max-w-[80%]",
                  isMe ? "ml-auto flex-row-reverse" : ""
                )}
              >
                {!isMe && (
                  <div className="w-8 shrink-0">
                    {showAvatar ? (
                      <Avatar className="w-8 h-8">
                        <AvatarImage src={selectedUser.avatar_url} />
                        <AvatarFallback className="text-xs">{selectedUser.username?.[0]}</AvatarFallback>
                      </Avatar>
                    ) : <div className="w-8" />}
                  </div>
                )}
                
                <div className={cn(
                  "flex flex-col gap-1 p-3 shadow-sm",
                  isMe 
                    ? "bg-primary text-primary-foreground rounded-2xl rounded-tr-sm" 
                    : "bg-background border text-foreground rounded-2xl rounded-tl-sm"
                )}>
                  {!isMe && showAvatar && (
                    <div className="text-[10px] font-bold opacity-70 mb-0.5">
                      {selectedUser.username}
                    </div>
                  )}
                  <div className="break-words text-sm">{msg.content}</div>
                  <div className={cn("text-[10px] opacity-70 text-right mt-1")}>
                    {format(new Date(msg.created_at), 'HH:mm')}
                  </div>
                </div>
              </div>
            );
          })}
          <div ref={scrollRef} />
        </div>
      </ScrollArea>

      <div className="p-4 border-t bg-background">
        {friendshipStatus === 'accepted' ? (
          <form onSubmit={handleSend} className="flex gap-2">
            <Input 
              value={newMessage}
              onChange={(e) => setNewMessage(e.target.value)}
              placeholder={t('typeMessage')}
              className="flex-1 rounded-full"
            />
            <Button type="submit" size="icon" className="rounded-full shrink-0" disabled={!newMessage.trim()}>
              <Send className="h-4 w-4" />
              <span className="sr-only">{t('send')}</span>
            </Button>
          </form>
        ) : (
          <div className="flex items-center justify-center gap-2 text-muted-foreground p-2 bg-muted/20 rounded-lg">
            <UserX className="h-4 w-4" />
            <span className="text-sm">You must be friends to chat.</span>
          </div>
        )}
      </div>
    </div>
  );
};
