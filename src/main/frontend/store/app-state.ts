import { signal } from '@vaadin/hilla-react-signals';
import { ScheduleEndpoint } from '../generated/endpoints';
import type SavedScheduleDTO from '../generated/com/sergofoox/domain/ui/dto/SavedScheduleDTO';

export interface SelectedEntity {
  id: number;
  type: 'GROUP' | 'TEACHER' | 'ROOM';
}

export const selectedEntity = signal<SelectedEntity | null>(null);

export const scheduleData = signal<any | null>(null);
export const scheduleLoading = signal(false);
export const isPublished = signal(false);
export const isBaseTemplateLocked = signal(false);
export const solverStatus = signal<string>('NOT_SOLVING');
export type CourseFilter = number | 'ALL';
export const selectedCourseFilter = signal<CourseFilter>(1);
export const activeSavedSchedule = signal<SavedScheduleDTO | null>(null);

export const BASE_TEMPLATE_LOCKED_MESSAGE = 'Для цього потрібно скопіювати шаблон.';

export function isTemplateEditBlocked() {
  return isBaseTemplateLocked.value;
}

export function getMutationErrorMessage(error: unknown, fallback: string) {
  const message =
    typeof error === 'object' && error !== null && 'message' in error
      ? String((error as { message?: unknown }).message || '')
      : '';
  return message.includes(BASE_TEMPLATE_LOCKED_MESSAGE) ? BASE_TEMPLATE_LOCKED_MESSAGE : fallback;
}

export async function refreshSchedule(showLoading = true) {
  if (showLoading) {
    scheduleLoading.value = true;
  }
  try {
    // 1. Спочатку завантажуємо основні дані розкладу (це критично для Grid)
    const result = await ScheduleEndpoint.getScheduleGridData();
    scheduleData.value = result;

    // 2. Потім завантажуємо інші статуси асинхронно
    Promise.all([
      ScheduleEndpoint.isPublished().then(v => isPublished.value = v),
      ScheduleEndpoint.getSolverStatus().then(v => solverStatus.value = v as any),
      ScheduleEndpoint.isBaseTemplateLocked().then(v => isBaseTemplateLocked.value = !!v),
      ScheduleEndpoint.getActiveSavedScheduleId().then(async (activeId) => {
        const schedules = await ScheduleEndpoint.getSavedSchedules();
        const current = (schedules || []).find((s: any) => s && s.id === activeId) || null;
        activeSavedSchedule.value = current;
      })
    ]).catch(err => console.warn('Non-critical status update failed:', err));
    
  } catch (err) {
    console.error('Критична помилка завантаження розкладу:', err);
    // Якщо дані вже були, не зануляємо їх при помилці оновлення
    if (!scheduleData.value) {
        scheduleData.value = null; 
    }
  } finally {
    if (showLoading) {
      scheduleLoading.value = false;
    }
  }
}
