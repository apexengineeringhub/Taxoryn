import { useEffect, useRef, useState, useCallback } from 'react';
import { EmployeeChatMessage } from '../api/employeeChatApi';

interface UseEmployeeChatOptions {
  currentEmployeeId?: string;
  onNewMessage?: (message: EmployeeChatMessage) => void;
  onMessagesRead?: (senderEmployeeId?: string, recipientEmployeeId?: string) => void;
  onTypingStatusChange?: (senderEmployeeId: string, isTyping: boolean) => void;
}

const resolveWebSocketBaseUrl = (): string => {
  if (import.meta.env.VITE_WS_URL) {
    return import.meta.env.VITE_WS_URL;
  }
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
  if (apiBaseUrl && (apiBaseUrl.startsWith('http://') || apiBaseUrl.startsWith('https://'))) {
    const wsProto = apiBaseUrl.startsWith('https://') ? 'wss:' : 'ws:';
    const parsed = new URL(apiBaseUrl);
    return `${wsProto}//${parsed.host}`;
  }
  if (typeof window !== 'undefined') {
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      return `${protocol}//${window.location.host}`;
    }
  }
  // Production Cloud Deployment (direct Render connection)
  return 'wss://taxoryn.onrender.com';
};

export const useEmployeeChat = ({
  currentEmployeeId,
  onNewMessage,
  onMessagesRead,
  onTypingStatusChange,
}: UseEmployeeChatOptions) => {
  const socketRef = useRef<WebSocket | null>(null);
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [isConnecting, setIsConnecting] = useState<boolean>(false);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const pingIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const typingTimeoutsRef = useRef<Map<string, NodeJS.Timeout>>(new Map());

  const onNewMessageRef = useRef(onNewMessage);
  onNewMessageRef.current = onNewMessage;

  const onMessagesReadRef = useRef(onMessagesRead);
  onMessagesReadRef.current = onMessagesRead;

  const onTypingRef = useRef(onTypingStatusChange);
  onTypingRef.current = onTypingStatusChange;

  const connect = useCallback(() => {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (!token) {
      setIsConnected(false);
      return;
    }

    if (socketRef.current && (socketRef.current.readyState === WebSocket.OPEN || socketRef.current.readyState === WebSocket.CONNECTING)) {
      return;
    }

    try {
      setIsConnecting(true);
      const wsBase = resolveWebSocketBaseUrl();
      const wsUrl = `${wsBase}/ws/employee-chat?token=${encodeURIComponent(token)}`;

      const socket = new WebSocket(wsUrl);
      socketRef.current = socket;

      socket.onopen = () => {
        setIsConnected(true);
        setIsConnecting(false);

        // Ping keepalive every 25 seconds
        if (pingIntervalRef.current) clearInterval(pingIntervalRef.current);
        pingIntervalRef.current = setInterval(() => {
          if (socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({ type: 'PING' }));
          }
        }, 25000);
      };

      socket.onmessage = (event) => {
        try {
          const payload = JSON.parse(event.data);
          if (payload.type === 'NEW_MESSAGE' && payload.message) {
            onNewMessageRef.current?.(payload.message);
          } else if (payload.type === 'MESSAGES_READ') {
            onMessagesReadRef.current?.(payload.senderEmployeeId, payload.recipientEmployeeId);
          } else if (payload.type === 'TYPING_START' && payload.senderEmployeeId) {
            onTypingRef.current?.(payload.senderEmployeeId, true);
            // Clear previous timeout and auto stop typing indicator after 4s
            if (typingTimeoutsRef.current.has(payload.senderEmployeeId)) {
              clearTimeout(typingTimeoutsRef.current.get(payload.senderEmployeeId)!);
            }
            const timeout = setTimeout(() => {
              onTypingRef.current?.(payload.senderEmployeeId, false);
              typingTimeoutsRef.current.delete(payload.senderEmployeeId);
            }, 4000);
            typingTimeoutsRef.current.set(payload.senderEmployeeId, timeout);
          } else if (payload.type === 'TYPING_STOP' && payload.senderEmployeeId) {
            if (typingTimeoutsRef.current.has(payload.senderEmployeeId)) {
              clearTimeout(typingTimeoutsRef.current.get(payload.senderEmployeeId)!);
              typingTimeoutsRef.current.delete(payload.senderEmployeeId);
            }
            onTypingRef.current?.(payload.senderEmployeeId, false);
          }
        } catch {
          // Ignored
        }
      };

      socket.onclose = () => {
        setIsConnected(false);
        setIsConnecting(false);
        if (pingIntervalRef.current) clearInterval(pingIntervalRef.current);

        // Reconnect after 4s
        if (!reconnectTimeoutRef.current) {
          reconnectTimeoutRef.current = setTimeout(() => {
            reconnectTimeoutRef.current = null;
            connect();
          }, 4000);
        }
      };

      socket.onerror = () => {
        setIsConnected(false);
        setIsConnecting(false);
        try {
          socket.close();
        } catch {
          // Ignored
        }
      };
    } catch {
      setIsConnected(false);
      setIsConnecting(false);
    }
  }, []);

  useEffect(() => {
    connect();

    return () => {
      if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current);
      if (pingIntervalRef.current) clearInterval(pingIntervalRef.current);
      typingTimeoutsRef.current.forEach((t) => clearTimeout(t));
      if (socketRef.current) {
        try {
          socketRef.current.close();
        } catch {
          // Ignored
        }
      }
    };
  }, [connect]);

  const sendTypingStart = useCallback((recipientEmployeeId?: string, channelId?: string) => {
    if (socketRef.current && socketRef.current.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify({
        type: 'TYPING_START',
        recipientEmployeeId,
        channelId,
      }));
    }
  }, []);

  const sendTypingStop = useCallback((recipientEmployeeId?: string, channelId?: string) => {
    if (socketRef.current && socketRef.current.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify({
        type: 'TYPING_STOP',
        recipientEmployeeId,
        channelId,
      }));
    }
  }, []);

  return {
    isConnected,
    isConnecting,
    sendTypingStart,
    sendTypingStop,
    reconnect: connect,
  };
};
