type StoredActiveSchedule = {
  id: number;
  name?: string;
  timestamp: number;
};

const ACTIVE_SCHEDULE_KEY = 'schedule-system:active-schedule';
const maxStoredAgeMs = 30 * 24 * 60 * 60 * 1000;

const isStoredActiveSchedule = (value: unknown): value is StoredActiveSchedule => {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const stored = value as Partial<StoredActiveSchedule>;
  return typeof stored.id === 'number' && stored.id > 0 && typeof stored.timestamp === 'number';
};

export const getStoredActiveSchedule = () => {
  if (typeof window === 'undefined') {
    return null;
  }
  try {
    const raw = localStorage.getItem(ACTIVE_SCHEDULE_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw);
    if (!isStoredActiveSchedule(parsed) || Date.now() - parsed.timestamp > maxStoredAgeMs) {
      localStorage.removeItem(ACTIVE_SCHEDULE_KEY);
      return null;
    }
    return parsed;
  } catch (error) {
    console.warn('Failed to read active schedule recovery cache:', error);
    return null;
  }
};

export const rememberActiveSchedule = (schedule: { id?: number; name?: string } | null | undefined) => {
  if (typeof window === 'undefined' || !schedule?.id || schedule.id <= 0) {
    return;
  }
  try {
    const stored: StoredActiveSchedule = {
      id: schedule.id,
      name: schedule.name,
      timestamp: Date.now()
    };
    localStorage.setItem(ACTIVE_SCHEDULE_KEY, JSON.stringify(stored));
  } catch (error) {
    console.warn('Failed to write active schedule recovery cache:', error);
  }
};

export const forgetStoredActiveSchedule = (id?: number) => {
  if (typeof window === 'undefined') {
    return;
  }
  if (id !== undefined) {
    const stored = getStoredActiveSchedule();
    if (!stored || stored.id !== id) {
      return;
    }
  }
  localStorage.removeItem(ACTIVE_SCHEDULE_KEY);
};
