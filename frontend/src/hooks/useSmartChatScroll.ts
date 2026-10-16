import { useEffect, useRef, useState, useCallback, MutableRefObject } from 'react';

export interface UseSmartChatScrollOptions {
  /**
   * Distance in pixels from the bottom of the container within which
   * the view is considered "at the bottom". Defaults to 100px.
   */
  threshold?: number;
  nearBottomThreshold?: number;
  /**
   * Array of dependencies or single dependency that triggers scroll re-evaluation (typically messages list, typing indicators).
   */
  dependencies?: any[];
  dependency?: any;
  /**
   * Flag indicating if the current user just sent a message (forcing auto-scroll to bottom).
   */
  isOwnMessageSent?: boolean;
  smooth?: boolean;
}

export interface UseSmartChatScrollReturn {
  containerRef: MutableRefObject<HTMLDivElement | null>;
  scrollContainerRef: MutableRefObject<HTMLDivElement | null>;
  messagesEndRef: MutableRefObject<HTMLDivElement | null>;
  isNearBottom: boolean;
  unreadBelow: number;
  unreadBelowCount: number;
  scrollToBottom: (smooth?: boolean) => void;
  handleScroll: () => void;
}

export function useSmartChatScroll({
  threshold,
  nearBottomThreshold,
  dependencies,
  dependency,
  isOwnMessageSent = false,
  smooth = true,
}: UseSmartChatScrollOptions): UseSmartChatScrollReturn {
  const effectiveThreshold = nearBottomThreshold ?? threshold ?? 100;
  const deps = dependencies ?? (dependency !== undefined ? [dependency] : []);

  const containerRef = useRef<HTMLDivElement | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const [isNearBottom, setIsNearBottom] = useState<boolean>(true);
  const [unreadBelow, setUnreadBelow] = useState<number>(0);
  const prevCountRef = useRef<number>(0);
  const isInitialMount = useRef<boolean>(true);

  const checkIfNearBottom = useCallback(() => {
    const el = containerRef.current;
    if (!el) return true;
    const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    return distanceToBottom <= effectiveThreshold;
  }, [effectiveThreshold]);

  const scrollToBottom = useCallback((isSmooth = smooth) => {
    if (containerRef.current) {
      containerRef.current.scrollTo({
        top: containerRef.current.scrollHeight,
        behavior: isSmooth ? 'smooth' : 'auto',
      });
      setIsNearBottom(true);
      setUnreadBelow(0);
    } else if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: isSmooth ? 'smooth' : 'auto' });
      setIsNearBottom(true);
      setUnreadBelow(0);
    }
  }, [smooth]);

  const handleScroll = useCallback(() => {
    const near = checkIfNearBottom();
    setIsNearBottom(near);
    if (near) {
      setUnreadBelow(0);
    }
  }, [checkIfNearBottom]);

  useEffect(() => {
    const primaryDep = deps[0];
    const currentLength = Array.isArray(primaryDep) ? primaryDep.length : 0;
    const isNewMessage = currentLength > prevCountRef.current;
    prevCountRef.current = currentLength;

    if (isInitialMount.current) {
      scrollToBottom(false);
      isInitialMount.current = false;
      return;
    }

    if (isOwnMessageSent) {
      scrollToBottom(true);
      return;
    }

    if (isNearBottom) {
      scrollToBottom(true);
    } else if (isNewMessage) {
      setUnreadBelow((prev) => prev + 1);
    }
  }, deps);

  return {
    containerRef,
    scrollContainerRef: containerRef,
    messagesEndRef,
    isNearBottom,
    unreadBelow,
    unreadBelowCount: unreadBelow,
    scrollToBottom,
    handleScroll,
  };
}
