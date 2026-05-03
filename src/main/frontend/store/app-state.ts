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
export const solverStatus = signal<string>('NOT_SOLVING');
export type CourseFilter = number | 'ALL';
export const selectedCourseFilter = signal<CourseFilter>(1);

export async function refreshSchedule(showLoading = true) {

  if (showLoading) {
    scheduleLoading.value = true;
  }
  try {
    const [result, publishedStatus, status] = await Promise.all([
      ScheduleEndpoint.getScheduleGridData(),
      ScheduleEndpoint.isPublished(),
      ScheduleEndpoint.getSolverStatus()
    ]);
    scheduleData.value = result;
    isPublished.value = publishedStatus;
    solverStatus.value = status as any;
  } finally {
    if (showLoading) {
      scheduleLoading.value = false;
    }
  }
}
