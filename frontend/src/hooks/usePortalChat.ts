import { useEffect, useRef, useState, useCallback } from 'react';
import { getAccessToken } from '../api/client';
import { ClientPortalMessage } from '../types';

interface UsePortalChatOptions {
  clientId?: string;
  isPracticeUser?: boolean;
  onMessageReceived?: (message: ClientPortalMessage) => void;
  onMessagesRead?: () => void;
}

export const usePortalChat = ({
  clientId,
  isPracticeUser,
  onMessageReceived,
  onMessagesRead,
}: UsePortalChatOptions) => {
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [isTyping, setIsTyping] = useState<boolean>(false);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const pingIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const typingTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const onMessageReceivedRef = useRef(onMessageReceived);
  const onMessagesReadRef = useRef(onMessagesRead);

  useEffect(() => {
    onMessageReceivedRef.current = onMessageReceived;
    onMessagesReadRef.current = onMessagesRead;
  }, [onMessageReceived, onMessagesRead]);

  const connect = useCallback(() => {
    const token = getAccessToken();
    if (!token) return;

    if (isPracticeUser && !clientId) return;

    // Avoid duplicate connection if already open or connecting
    if (wsRef.current && (wsRef.current.readyState === WebSocket.OPEN || wsRef.current.readyState === WebSocket.CONNECTING)) {
      return;
    }

    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.host;
      const targetClientId = clientId || '';
      const wsUrl = `${protocol}//${host}/ws/portal-chat?token=${encodeURIComponent(token)}${
        targetClientId ? `&clientId=${encodeURIComponent(targetClientId)}` : ''
      }`;

      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        setIsConnected(true);
        if (pingIntervalRef.current) clearInterval(pingIntervalRef.current);
        // Start ping interval every 25 seconds to keep connection alive
        pingIntervalRef.current = setInterval(() => {
          if (ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ type: 'PING' }));
          }
        }, 25000);
      };

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          if (data.type === 'MESSAGE_RECEIVED' && data.message) {
            onMessageReceivedRef.current?.(data.message);
          } else if (data.type === 'MESSAGES_READ') {
            onMessagesReadRef.current?.();
          } else if (data.type === 'TYPING_START') {
            setIsTyping(true);
            if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
            typingTimeoutRef.current = setTimeout(() => setIsTyping(false), 3000);
          } else if (data.type === 'TYPING_STOP') {
            setIsTyping(false);
          }
        } catch {
          // ignore malformed frame
        }
      };

      ws.onclose = () => {
        setIsConnected(false);
        if (pingIntervalRef.current) clearInterval(pingIntervalRef.current);
        // Auto-reconnect after 4 seconds if unmounted cleanly
        if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = setTimeout(() => {
          connect();
        }, 4000);
      };

      ws.onerror = () => {
        try {
          ws.close();
        } catch {}
      };
    } catch {
      setIsConnected(false);
    }
  }, [clientId, isPracticeUser]);

  useEffect(() => {
    connect();

    return () => {
      if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current);
      if (pingIntervalRef.current) clearInterval(pingIntervalRef.current);
      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [connect]);

  const sendTyping = (typing: boolean) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(
        JSON.stringify({
          type: typing ? 'TYPING_START' : 'TYPING_STOP',
        })
      );
    }
  };

  return {
    isConnected,
    isTyping,
    sendTyping,
  };
};
