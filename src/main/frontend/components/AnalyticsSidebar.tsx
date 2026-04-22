import React, { useEffect, useState } from 'react';
import { ScheduleEndpoint } from '../generated/endpoints';
import type ScheduleAnalyticsDTO from '../generated/com/sergofoox/domain/ui/dto/ScheduleAnalyticsDTO';
import { selectedEntity } from '../store/app-state';
import { ProgressBar } from '@vaadin/react-components/ProgressBar.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { Button } from '@vaadin/react-components/Button.js';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';

export const AnalyticsSidebar: React.FC = () => {
  const [analytics, setAnalytics] = useState<ScheduleAnalyticsDTO | null>(null);
  const [loading, setLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    if (selectedEntity.value) {
      setLoading(true);
      ScheduleEndpoint.getAnalytics(
        selectedEntity.value.id as any,
        selectedEntity.value.type
      ).then(data => {
        setAnalytics(data);
        setLoading(false);
      }).catch(err => {
        console.error("Failed to fetch analytics", err);
        setLoading(false);
      });
    } else {
      setAnalytics(null);
    }
  }, [selectedEntity.value]);

  if (!isOpen) {
    return (
      <div className="fixed right-0 top-1/2 -translate-y-1/2 z-50">
        <Button 
          theme="primary icon" 
          onClick={() => setIsOpen(true)}
          style={{ borderRadius: '50% 0 0 50%', height: '60px', width: '40px' }}
        >
          <Icon icon="lumo:bar-chart" />
        </Button>
      </div>
    );
  }

  return (
    <div className="w-80 h-full bg-[var(--aura-surface-color)] border-l flex flex-col shadow-xl transition-all duration-300 ease-in-out relative">
      <div className="p-4 border-b flex justify-between items-center bg-gray-50">
        <h3 className="text-lg font-bold flex items-center gap-2">
          <Icon icon="lumo:bar-chart" className="text-blue-600" />
          Аналітика
        </h3>
        <Button theme="tertiary icon" onClick={() => setIsOpen(false)}>
          <Icon icon="lumo:cross" />
        </Button>
      </div>

      <div className="flex-grow overflow-y-auto p-4">
        {!selectedEntity.value ? (
          <div className="text-center text-gray-500 py-8 flex flex-col items-center gap-4">
            <Icon icon="lumo:search" style={{ fontSize: '48px', opacity: 0.3 }} />
            <p>Оберіть групу, викладача або аудиторію для перегляду аналітики</p>
          </div>
        ) : loading ? (
          <div className="space-y-4">
             <div className="h-20 bg-gray-100 animate-pulse rounded"></div>
             <div className="h-60 bg-gray-100 animate-pulse rounded"></div>
             <div className="h-60 bg-gray-100 animate-pulse rounded"></div>
          </div>
        ) : analytics ? (
          <div className="space-y-6">
            <div className="p-2 rounded-md bg-blue-50 border border-blue-100">
              <div className="text-xs uppercase text-gray-500 font-semibold">Вибраний об'єкт</div>
              <div className="text-xl font-bold text-blue-600">{analytics.entityName}</div>
              <div className="text-xs text-gray-500 mt-1">
                {analytics.entityType === 'GROUP' ? 'Група' : 
                 analytics.entityType === 'TEACHER' ? 'Викладач' : 'Аудиторія'}
              </div>
            </div>

            <div className="space-y-4">
              <h4 className="font-bold border-b pb-1">Виконання плану (год)</h4>
              {analytics.courses && analytics.courses.length > 0 ? (
                analytics.courses.filter(c => !!c).map((course, idx) => (
                  <div key={idx} className="space-y-1 p-2 border rounded bg-[var(--aura-surface-color)]">
                    <div className="flex justify-between text-s">
                      <span className="font-medium truncate flex-grow mr-2" title={course.subjectName || ''}>
                        {course.subjectName}
                      </span>
                      <span className="font-bold whitespace-nowrap">
                        {course.executedHours} / {course.totalHours}
                      </span>
                    </div>
                    <ProgressBar value={(course.percentage || 0) / 100} />
                    <div className="text-[10px] text-right text-gray-500">
                      {Math.round(course.percentage || 0)}% завершено
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-s text-gray-500 italic">Навчальні плани не знайдені</div>
              )}
            </div>

            <div className="space-y-4">
              <h4 className="font-bold border-b pb-1">Якість розкладу</h4>
              <div className="flex items-center justify-between p-2 border rounded bg-[var(--aura-surface-color)]">
                <span className="text-s">Кількість "вікон" (пауз)</span>
                <span className={`px-2 py-1px rounded-full text-xs font-bold ${
                  analytics.totalWindows === 0 ? 'bg-green-50 text-green-500' : 
                  analytics.totalWindows < 3 ? 'bg-yellow-50 text-yellow-600' : 'bg-red-50 text-red-500'
                }`}>
                  {analytics.totalWindows}
                </span>
              </div>
              {analytics.totalWindows > 0 && (
                <div className="text-xs text-red-500 flex items-center gap-1">
                  <Icon icon="lumo:error" style={{ fontSize: '12px' }} />
                  <span>Виявлено небажані перерви в розкладі</span>
                </div>
              )}
            </div>
          </div>
        ) : (
          <div className="text-red-500">Помилка завантаження даних</div>
        )}
      </div>
      
      <div className="p-4 border-t bg-gray-50 text-[10px] text-gray-500 text-center">
        ASMS v3.0 | Система управління розкладом
      </div>
    </div>
  );
};
