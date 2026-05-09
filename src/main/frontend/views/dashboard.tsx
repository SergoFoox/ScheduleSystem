import React, { useEffect } from 'react';
import { Select } from '@vaadin/react-components/Select.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { ProgressBar } from '@vaadin/react-components/ProgressBar.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { ScheduleGrid } from '../components/ScheduleGrid';
import { AnalyticsSidebar } from '../components/AnalyticsSidebar';
import { SavedSchedulesPanel } from '../components/SavedSchedulesPanel';
import { useSignal } from '@vaadin/hilla-react-signals';
import {
  BASE_TEMPLATE_LOCKED_MESSAGE,
  getMutationErrorMessage,
  isBaseTemplateLocked,
  isPublished,
  refreshSchedule,
  selectedCourseFilter,
  selectedEntity,
  solverStatus
} from '../store/app-state';
import { ScheduleEndpoint } from '../generated/endpoints';

type Mode = 'GROUP' | 'TEACHER' | 'ROOM';

export default function DashboardView() {
  const mode = useSignal<Mode>('GROUP');
  const isSolving = solverStatus.value === 'SOLVING_ACTIVE' || solverStatus.value === 'SOLVING_SCHEDULED';

  // Poll status when solving to see updates in real-time
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
    } catch (err) {
      console.error(err);
      Notification.show(getMutationErrorMessage(err, 'Помилка під час очищення розкладу'), { theme: 'error' });
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
        {/* Modern Toolbar */}
        <div className="flex justify-between items-center p-4 bg-white border-b shadow-sm gap-4">
          <div className="flex items-center gap-6">
            <h2 className="text-2xl font-extrabold tracking-tight text-gray-900 whitespace-nowrap">
              Розклад занять
            </h2>
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-gray-500 uppercase">Режим:</span>
              <Select
                value={mode.value}
                items={modeOptions}
                onValueChanged={(e) => {
                  mode.value = e.detail.value as Mode;
                  selectedEntity.value = null;
                }}
                className="w-44"
              />
            </div>
          </div>

          <div className="flex items-center gap-4">
            {isSolving && (
              <div className="flex flex-col items-end mr-4 min-w-48">
                <div className="flex items-center gap-2 mb-1">
                   <Icon icon="vaadin:refresh" className="w-3 h-3 text-blue-600 animate-spin" />
                   <span className="text-[10px] font-bold text-blue-600 uppercase tracking-widest">
                    Оптимізація Timefold...
                  </span>
                </div>
                <ProgressBar indeterminate className="w-full h-1" />
              </div>
            )}
            
            <Button 
              theme="primary" 
              onClick={handleGenerate} 
              disabled={isSolving || isPublished.value}
              className="shadow-md transition-transform active:scale-95"
            >
              <Icon icon="vaadin:play" slot="prefix" />
              Згенерувати
            </Button>

            <div className="flex gap-2 border-l pl-4 ml-2">
              <Button 
                theme="tertiary" 
                onClick={() => Notification.show('Експорт HTML у розробці', { position: 'bottom-center' })}
                title="Експорт у HTML"
              >
                <Icon icon="vaadin:download" slot="prefix" />
                HTML
              </Button>
              <Button 
                theme="tertiary" 
                onClick={() => Notification.show('Експорт PDF у розробці', { position: 'bottom-center' })}
                title="Експорт у PDF"
              >
                <Icon icon="vaadin:download" slot="prefix" />
                PDF
              </Button>
            </div>
            
            <Button 
              theme="error tertiary" 
              onClick={handleClear}
              disabled={isSolving || isPublished.value}
              className="hover:bg-red-50"
            >
              <Icon icon="vaadin:trash" slot="prefix" />
              Очистити
            </Button>
          </div>
        </div>
        
        {/* Grid Container */}
        <div className="flex-1 overflow-hidden p-6">
          <div className="h-full border border-gray-200 rounded-2xl shadow-xl bg-white overflow-hidden">
            <ScheduleGrid />
          </div>
        </div>
      </div>
      
      <AnalyticsSidebar />
    </div>
  );
}
