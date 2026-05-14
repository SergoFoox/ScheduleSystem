import { useEffect, useRef } from 'react';

export type DataSyncScope =
  | 'all'
  | 'schedule'
  | 'savedSchedules'
  | 'teachers'
  | 'subjects'
  | 'groups'
  | 'rooms'
  | 'coursePlans'
  | 'competences'
  | 'availability'
  | 'autosave';

export interface DataSyncEvent {
  id: string;
  sourceTabId: string;
  scope: DataSyncScope;
  timestamp: number;
}

const CHANNEL_NAME = 'schedule-system:data-sync';
const STORAGE_KEY = 'schedule-system:last-data-sync-event';
const debounceMs = 150;

const createId = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
};

const tabId = createId();
const listeners = new Set<(event: DataSyncEvent) => void>();
const seenEventIds = new Set<string>();
let channel: BroadcastChannel | undefined;
let initialized = false;

const rememberEvent = (eventId: string) => {
  seenEventIds.add(eventId);
  if (seenEventIds.size <= 200) {
    return;
  }
  const [oldest] = seenEventIds;
  if (oldest) {
    seenEventIds.delete(oldest);
  }
};

const isDataSyncEvent = (value: unknown): value is DataSyncEvent => {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const event = value as Partial<DataSyncEvent>;
  return (
    typeof event.id === 'string' &&
    typeof event.sourceTabId === 'string' &&
    typeof event.scope === 'string' &&
    typeof event.timestamp === 'number'
  );
};

const dispatchIncomingEvent = (event: DataSyncEvent) => {
  if (event.sourceTabId === tabId || seenEventIds.has(event.id)) {
    return;
  }
  rememberEvent(event.id);
  listeners.forEach((listener) => listener(event));
};

const initialize = () => {
  if (initialized || typeof window === 'undefined') {
    return;
  }
  initialized = true;

  if ('BroadcastChannel' in window) {
    channel = new BroadcastChannel(CHANNEL_NAME);
    channel.onmessage = (message) => {
      if (isDataSyncEvent(message.data)) {
        dispatchIncomingEvent(message.data);
      }
    };
  }

  window.addEventListener('storage', (event) => {
    if (event.key !== STORAGE_KEY || !event.newValue) {
      return;
    }
    try {
      const payload = JSON.parse(event.newValue);
      if (isDataSyncEvent(payload)) {
        dispatchIncomingEvent(payload);
      }
    } catch (error) {
      console.warn('Failed to parse cross-tab sync event:', error);
    }
  });
};

export const notifyDataChanged = (scope: DataSyncScope = 'all') => {
  if (typeof window === 'undefined') {
    return;
  }
  initialize();

  const event: DataSyncEvent = {
    id: createId(),
    sourceTabId: tabId,
    scope,
    timestamp: Date.now()
  };
  rememberEvent(event.id);

  channel?.postMessage(event);

  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(event));
  } catch (error) {
    console.warn('Failed to write cross-tab sync event:', error);
  }
};

export const subscribeToDataSync = (listener: (event: DataSyncEvent) => void) => {
  initialize();
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
};

export const useCrossTabRefresh = (callback: (event: DataSyncEvent) => void | Promise<void>) => {
  const callbackRef = useRef(callback);
  const timeoutRef = useRef<number | undefined>(undefined);
  const latestEventRef = useRef<DataSyncEvent | undefined>(undefined);

  useEffect(() => {
    callbackRef.current = callback;
  }, [callback]);

  useEffect(() => {
    return subscribeToDataSync((event) => {
      latestEventRef.current = event;
      if (timeoutRef.current !== undefined) {
        window.clearTimeout(timeoutRef.current);
      }
      timeoutRef.current = window.setTimeout(() => {
        timeoutRef.current = undefined;
        const latestEvent = latestEventRef.current;
        if (latestEvent) {
          void callbackRef.current(latestEvent);
        }
      }, debounceMs);
    });
  }, []);

  useEffect(() => {
    return () => {
      if (timeoutRef.current !== undefined) {
        window.clearTimeout(timeoutRef.current);
      }
    };
  }, []);
};
