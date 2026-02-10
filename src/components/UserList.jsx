import React, { useEffect, useState } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from '../contexts/AuthContext';
import { useLanguage } from '../contexts/LanguageContext';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { ScrollArea } from '@/components/ui/scroll-area';
import { cn } from '@/lib/utils';
import { Search, UserPlus, Check, X, MessageSquare, Loader2 } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';

export const UserList = ({ onSelectUser, selectedUserId }) => {
  const { user } = useAuth();
  const { t } = useLanguage();
  const [friends, setFriends] = useState([]);
  const [requests, setRequests] = useState([]);
  const [searchResults, setSearchResults] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);
  const [sentRequests, setSentRequests] = useState(new Set());

  // Fetch initial data
  useEffect(() => {
    if (!user) return;

    const fetchData = async () => {
      setLoading(true);
      
      // Fetch Friends
      const { data: friendsData } = await supabase
        .from('friendships')
        .select(`
          id,
          user_id,
          friend_id,
          status,
          sender:user_id(id, username, full_name, avatar_url, status, last_seen),
          receiver:friend_id(id, username, full_name, avatar_url, status, last_seen)
        `)
        .or(`user_id.eq.${user.id},friend_id.eq.${user.id}`)
        .eq('status', 'accepted');

      if (friendsData) {
        const mappedFriends = friendsData.map(f => {
          const isSender = f.user_id === user.id;
          return {
            friendshipId: f.id,
            ...(isSender ? f.receiver : f.sender)
          };
        });
        setFriends(mappedFriends);
      }

      // Fetch Incoming Requests
      const { data: requestsData } = await supabase
        .from('friendships')
        .select(`
          id,
          status,
          created_at,
          sender:user_id(id, username, full_name, avatar_url)
        `)
        .eq('friend_id', user.id)
        .eq('status', 'pending');

      if (requestsData) {
        setRequests(requestsData);
      }

      // Fetch Sent Requests (to disable "Add" button)
      const { data: sentData } = await supabase
        .from('friendships')
        .select('friend_id')
        .eq('user_id', user.id)
        .eq('status', 'pending');
      
      if (sentData) {
        setSentRequests(new Set(sentData.map(r => r.friend_id)));
      }

      setLoading(false);
    };

    fetchData();

    // Realtime subscriptions
    const channel = supabase.channel('friendships_channel')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'friendships' }, () => {
        // Simple strategy: refetch all on any change
        fetchData();
      })
      .subscribe();

    return () => {
      supabase.removeChannel(channel);
    };
  }, [user]);

  // Search Users
  useEffect(() => {
    const searchUsers = async () => {
      if (!search.trim()) {
        setSearchResults([]);
        return;
      }

      const { data } = await supabase
        .from('profiles')
        .select('*')
        .neq('id', user.id)
        .or(`username.ilike.%${search}%,full_name.ilike.%${search}%`)
        .limit(20);

      if (data) {
        // Filter out existing friends
        const friendIds = new Set(friends.map(f => f.id));
        setSearchResults(data.filter(u => !friendIds.has(u.id)));
      }
    };

    const timeoutId = setTimeout(searchUsers, 500);
    return () => clearTimeout(timeoutId);
  }, [search, user, friends]);

  const sendRequest = async (targetUserId) => {
    try {
      const { error } = await supabase
        .from('friendships')
        .insert({ user_id: user.id, friend_id: targetUserId });
      
      if (error) throw error;
      setSentRequests(prev => new Set(prev).add(targetUserId));
    } catch (error) {
      console.error('Error sending request:', error);
    }
  };

  const respondToRequest = async (friendshipId, accept) => {
    try {
      const status = accept ? 'accepted' : 'rejected';
      const { error } = await supabase
        .from('friendships')
        .update({ status })
        .eq('id', friendshipId);
      
      if (error) throw error;
    } catch (error) {
      console.error('Error responding to request:', error);
    }
  };

  return (
    <div className="flex flex-col h-full border-r bg-muted/10 w-full md:w-80">
      <Tabs defaultValue="friends" className="flex-1 flex flex-col">
        <div className="p-4 border-b">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="friends">{t('friends') || 'Friends'}</TabsTrigger>
            <TabsTrigger value="requests" className="relative">
              {t('requests') || 'Requests'}
              {requests.length > 0 && (
                <Badge variant="destructive" className="absolute -top-2 -right-2 h-5 w-5 flex items-center justify-center rounded-full p-0 text-[10px]">
                  {requests.length}
                </Badge>
              )}
            </TabsTrigger>
            <TabsTrigger value="add">{t('add') || 'Add'}</TabsTrigger>
          </TabsList>
        </div>

        <TabsContent value="friends" className="flex-1 p-0 m-0">
          <div className="p-4 border-b">
            <div className="relative">
              <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input 
                placeholder={t('search_friends') || "Search friends..."}
                className="pl-9" 
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>
          <ScrollArea className="flex-1 h-[calc(100vh-180px)]">
            <div className="flex flex-col gap-1 p-2">
              {friends.filter(f => 
                f.username?.toLowerCase().includes(search.toLowerCase()) || 
                f.full_name?.toLowerCase().includes(search.toLowerCase())
              ).length === 0 ? (
                <div className="p-8 text-center text-sm text-muted-foreground">
                  {friends.length === 0 ? "You have no friends yet." : "No friends found matching search."}
                </div>
              ) : (
                friends.filter(f => 
                  f.username?.toLowerCase().includes(search.toLowerCase()) || 
                  f.full_name?.toLowerCase().includes(search.toLowerCase())
                ).map((friend) => (
                  <button
                    key={friend.id}
                    onClick={() => onSelectUser(friend)}
                    className={cn(
                      "flex items-center gap-3 p-3 rounded-lg hover:bg-muted transition-colors text-left w-full",
                      selectedUserId === friend.id && "bg-muted"
                    )}
                  >
                    <div className="relative">
                      <Avatar>
                        <AvatarImage src={friend.avatar_url} />
                        <AvatarFallback>{friend.username ? friend.username[0].toUpperCase() : '?'}</AvatarFallback>
                      </Avatar>
                      <span className={cn(
                        "absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-background",
                        friend.status === 'online' ? "bg-green-500" : "bg-gray-400"
                      )} />
                    </div>
                    <div className="flex-1 overflow-hidden">
                      <div className="font-medium truncate">{friend.full_name || friend.username}</div>
                      <div className="text-xs text-muted-foreground truncate">
                        @{friend.username} • {friend.status === 'online' ? 'Online' : 'Offline'}
                      </div>
                    </div>
                    <MessageSquare className="h-4 w-4 text-muted-foreground opacity-50" />
                  </button>
                ))
              )}
            </div>
          </ScrollArea>
        </TabsContent>

        <TabsContent value="requests" className="flex-1 p-0 m-0">
          <ScrollArea className="flex-1 h-full">
            <div className="flex flex-col gap-2 p-4">
              {requests.length === 0 ? (
                <div className="p-8 text-center text-sm text-muted-foreground">
                  No pending requests.
                </div>
              ) : (
                requests.map((req) => (
                  <div key={req.id} className="flex items-center justify-between p-3 border rounded-lg bg-card">
                    <div className="flex items-center gap-3">
                      <Avatar>
                        <AvatarImage src={req.sender?.avatar_url} />
                        <AvatarFallback>{req.sender?.username?.[0].toUpperCase()}</AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="font-medium">{req.sender?.full_name || req.sender?.username}</div>
                        <div className="text-xs text-muted-foreground">@{req.sender?.username}</div>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button size="icon" variant="ghost" className="h-8 w-8 text-green-600 hover:text-green-700 hover:bg-green-100" onClick={() => respondToRequest(req.id, true)}>
                        <Check className="h-4 w-4" />
                      </Button>
                      <Button size="icon" variant="ghost" className="h-8 w-8 text-red-600 hover:text-red-700 hover:bg-red-100" onClick={() => respondToRequest(req.id, false)}>
                        <X className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </ScrollArea>
        </TabsContent>

        <TabsContent value="add" className="flex-1 p-0 m-0">
          <div className="p-4 border-b">
            <div className="relative">
              <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input 
                placeholder="Search by username..." 
                className="pl-9" 
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>
          <ScrollArea className="flex-1 h-[calc(100vh-180px)]">
            <div className="flex flex-col gap-2 p-4">
              {search.length < 2 ? (
                <div className="p-8 text-center text-sm text-muted-foreground">
                  Type at least 2 characters to search.
                </div>
              ) : searchResults.length === 0 ? (
                <div className="p-8 text-center text-sm text-muted-foreground">
                  No users found.
                </div>
              ) : (
                searchResults.map((u) => (
                  <div key={u.id} className="flex items-center justify-between p-3 border rounded-lg bg-card">
                    <div className="flex items-center gap-3">
                      <Avatar>
                        <AvatarImage src={u.avatar_url} />
                        <AvatarFallback>{u.username ? u.username[0].toUpperCase() : '?'}</AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="font-medium">{u.full_name || u.username}</div>
                        <div className="text-xs text-muted-foreground">@{u.username}</div>
                      </div>
                    </div>
                    {sentRequests.has(u.id) ? (
                      <Button size="sm" variant="ghost" disabled className="text-xs">
                        Sent
                      </Button>
                    ) : (
                      <Button size="sm" onClick={() => sendRequest(u.id)} className="gap-2">
                        <UserPlus className="h-4 w-4" />
                        Add
                      </Button>
                    )}
                  </div>
                ))
              )}
            </div>
          </ScrollArea>
        </TabsContent>
      </Tabs>
    </div>
  );
};
