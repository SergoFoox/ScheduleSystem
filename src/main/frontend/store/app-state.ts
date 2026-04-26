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

export async function refreshSchedule() {

  scheduleLoading.value = true;
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
    scheduleLoading.value = false;
  }
}
