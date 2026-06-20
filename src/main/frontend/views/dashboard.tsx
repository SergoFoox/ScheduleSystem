import React, { useEffect } from 'react';
import { Select } from '@vaadin/react-components/Select.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { ProgressBar } from '@vaadin/react-components/ProgressBar.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { Checkbox } from '@vaadin/react-components/Checkbox.js';
import { ScheduleGrid } from '../components/ScheduleGrid';
import { SavedSchedulesPanel } from '../components/SavedSchedulesPanel';
import { TimeMachineDialog } from '../components/TimeMachineDialog';
import { useSignal } from '@vaadin/hilla-react-signals';
import {
  activeSavedSchedule,
  BASE_TEMPLATE_LOCKED_MESSAGE,
  getMutationErrorMessage,
  isBaseTemplateLocked,
  refreshSchedule,
  scheduleData,
  selectedCourseFilter,
  solverStatus
} from '../store/app-state';
import { ScheduleEndpoint } from '../generated/endpoints';
import { notifyDataChanged } from '../utils/cross-tab-sync';
import { downloadScheduleHtml, downloadSchedulePdf } from '../utils/schedule-export';

type Mode = 'GROUP' | 'TEACHER' | 'ROOM';

export default function DashboardView() {
  const mode = useSignal<Mode>('GROUP');
  const timeMachineOpened = useSignal(false);
  const isSolving = solverStatus.value === 'SOLVING_ACTIVE' || solverStatus.value === 'SOLVING_SCHEDULED';

  // Poll the solver status while solving so the UI can show live updates.
  useEffect(() => {
    let interval: any;
    if (isSolving) {
      interval = setInterval(() => {
        refreshSchedule(false);
      }, 3000);
    }
    return () => interval && clearInterval(interval);
  }, [isSolving]);

  const handleGenerate = async () => {
    try {
      const courseFilter = selectedCourseFilter.value;
      const course = courseFilter === 'ALL' ? 0 : courseFilter;
      await ScheduleEndpoint.generateScheduleForCourse(course);
      Notification.show(course > 0 ? `Генерацію розкладу для ${course} курсу розпочато` : 'Генерацію розкладу для всіх курсів розпочато', {
        theme: 'success', 
        position: 'bottom-end' 
      });
      await refreshSchedule();
      notifyDataChanged('schedule');
      const pollUntil = Date.now() + 35000;
      const poll = window.setInterval(async () => {
        await refreshSchedule(false);
        if (solverStatus.value === 'NOT_SOLVING' || Date.now() > pollUntil) {
          window.clearInterval(poll);
          await refreshSchedule(false);
        }
      }, 1500);
    } catch (err) {
      console.error(err);
      Notification.show('Помилка під час запуску генерації', { theme: 'error' });
    }
  };

  const handleClear = async () => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    if (!window.confirm('Ви впевнені, що хочете повністю очистити поточний розклад?')) return;
    try {
      await ScheduleEndpoint.clearSchedule();
      Notification.show('Розклад очищено', { theme: 'success' });
      await refreshSchedule();
      notifyDataChanged('schedule');
    } catch (err) {
      console.error(err);
      Notification.show(getMutationErrorMessage(err, 'Помилка під час очищення розкладу'), { theme: 'error' });
    }
  };

  const getExportScheduleName = () => activeSavedSchedule.value?.name || 'Розклад занять';

  const hasExportableSchedule = () => {
    if (scheduleData.value?.groups?.length) {
      return true;
    }
    Notification.show('Немає даних розкладу для експорту', { theme: 'error', position: 'bottom-end' });
    return false;
  };

  const handleExportHtml = () => {
    if (!hasExportableSchedule()) return;
    downloadScheduleHtml(scheduleData.value, selectedCourseFilter.value, getExportScheduleName());
    Notification.show('HTML експортовано', { theme: 'success', position: 'bottom-end' });
  };

  const handleExportPdf = async () => {
    if (!hasExportableSchedule()) return;
    try {
      await downloadSchedulePdf(scheduleData.value, selectedCourseFilter.value, getExportScheduleName());
      Notification.show('PDF експортовано', { theme: 'success', position: 'bottom-end' });
    } catch (err) {
      console.error('Failed to export PDF:', err);
      Notification.show('Помилка під час експорту PDF', { theme: 'error', position: 'bottom-end' });
    }
  };

  const modeOptions = [
    { label: 'Групи', value: 'GROUP' },
    { label: 'Викладачі', value: 'TEACHER' },
    { label: 'Аудиторії', value: 'ROOM' },
  ];

  return (
    <div className="flex h-full overflow-hidden bg-gray-50/50">
      <SavedSchedulesPanel />
      <div className="flex-1 flex flex-col min-w-0">
        {/* Modern toolbar */}
        <div className="flex justify-between items-center p-4 bg-white border-b shadow-sm gap-4">
          <div className="flex items-center gap-6">
            <h2 className="text-2xl font-extrabold tracking-tight text-gray-900 whitespace-nowrap">
              Розклад занять
            </h2>
          </div>
          <div className="flex items-center gap-4">
            {isSolving && (
              <div className="flex flex-col items-end mr-4 min-w-[200px]">
                <div className="flex items-center gap-2 mb-1">
                   <Icon icon="vaadin:refresh" className="w-3 h-3 text-blue-600 animate-spin" />
                   <span className="text-[10px] font-bold text-blue-600 uppercase tracking-widest">
                    Оптимізація...
                  </span>
                </div>
                <ProgressBar indeterminate className="w-full h-1" />
              </div>
            )}
            
            <Button 
              theme="primary" 
              onClick={handleGenerate} 
              disabled={isSolving}
              className="shadow-md transition-transform active:scale-95"
            >
              <Icon icon="vaadin:play" slot="prefix" />
              <span className="hidden lg:inline">Згенерувати</span>
              <span className="lg:hidden">Пуск</span>
            </Button>

            {/* Autosave block with a clear label and history icon */}
            {!isBaseTemplateLocked.value && activeSavedSchedule.value && (
              <div className="flex items-center gap-4 border-l border-r px-4 min-w-fit">
                <Checkbox 
                  label="Автозбереження" 
                  className="whitespace-nowrap"
                  checked={activeSavedSchedule.value.autosaveEnabled}
                  onCheckedChanged={async (e) => {
                    const enabled = e.detail.value;
                    if (activeSavedSchedule.value?.id) {
                      try {
                        await ScheduleEndpoint.toggleAutosave(activeSavedSchedule.value.id, enabled);
                        await refreshSchedule(false);
                        notifyDataChanged('savedSchedules');
                        Notification.show(enabled ? 'Автозбереження увімкнено' : 'Автозбереження вимкнено', {
                          theme: enabled ? 'success' : 'contrast',
                          position: 'bottom-center'
                        });
                      } catch (err) {
                        Notification.show('Помилка', { theme: 'error' });
                      }
                    }
                  }}
                />
                
                <Button 
                  theme="tertiary" 
                  onClick={() => timeMachineOpened.value = true}
                  title="Історія автозбережень та ручних копій"
                  className="whitespace-nowrap"
                >
                  <Icon icon="vaadin:time-backward" slot="prefix" />
                  <span className="hidden xl:inline">Історія</span>
                </Button>
              </div>
            )}

            <div className="flex gap-1 items-center">
              <Button 
                theme="tertiary" 
                onClick={handleExportHtml}
                title="Експорт у HTML"
              >
                <Icon icon="vaadin:download" />
              </Button>
              <Button 
                theme="tertiary" 
                onClick={handleExportPdf}
                title="Експорт у PDF"
              >
                <Icon icon="vaadin:file-text" />
              </Button>
            </div>
            
            <Button 
              theme="error tertiary" 
              onClick={handleClear}
              disabled={isSolving}
              title="Очистити розклад"
            >
              <Icon icon="vaadin:trash" />
            </Button>
          </div>
        </div>
        
        {/* Grid container */}
        <div className="flex-1 overflow-hidden p-6">
          <div className="h-full border border-gray-200 rounded-2xl shadow-xl bg-white overflow-hidden relative">
            <ScheduleGrid />
          </div>
        </div>
      </div>

      <TimeMachineDialog 
        opened={timeMachineOpened.value} 
        onClose={() => timeMachineOpened.value = false} 
      />
    </div>
  );
}
