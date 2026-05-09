import { signal } from '@vaadin/hilla-react-signals';
import { ScheduleEndpoint } from '../generated/endpoints';

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
    const [result, publishedStatus, status, baseTemplateLocked] = await Promise.all([
      ScheduleEndpoint.getScheduleGridData(),
      ScheduleEndpoint.isPublished(),
      ScheduleEndpoint.getSolverStatus(),
      ScheduleEndpoint.isBaseTemplateLocked()
    ]);
    scheduleData.value = result;
    isPublished.value = publishedStatus;
    solverStatus.value = status as any;
    isBaseTemplateLocked.value = !!baseTemplateLocked;
  } finally {
    if (showLoading) {
      scheduleLoading.value = false;
    }
  }
}
