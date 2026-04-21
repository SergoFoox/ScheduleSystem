import { signal } from '@vaadin/hilla-react-signals';
import type ScheduleGridDTO from '../generated/com/sergofoox/domain/ui/dto/ScheduleGridDTO';
import { ScheduleEndpoint } from '../generated/endpoints';

export interface SelectedEntity {
  id: number;
  type: 'GROUP' | 'TEACHER' | 'ROOM';
}

export const selectedEntity = signal<SelectedEntity | null>(null);

export const scheduleData = signal<ScheduleGridDTO | null>(null);
export const scheduleLoading = signal(true);
export const isPublished = signal(false);

export async function refreshSchedule() {
  scheduleLoading.value = true;
  try {
    const [result, publishedStatus] = await Promise.all([
      ScheduleEndpoint.getScheduleGridData(),
      ScheduleEndpoint.isPublished()
    ]);
    scheduleData.value = result;
    isPublished.value = publishedStatus;
  } finally {
    scheduleLoading.value = false;
  }
}
