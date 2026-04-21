import React from 'react';
import { Select } from '@vaadin/react-components/Select.js';
import { ScheduleGrid } from '../components/ScheduleGrid';
import { AnalyticsSidebar } from '../components/AnalyticsSidebar';
import { useSignal } from '@vaadin/hilla-react-signals';
import { selectedEntity } from '../store/app-state';

type Mode = 'GROUP' | 'TEACHER' | 'ROOM';

export default function DashboardView() {
  const mode = useSignal<Mode>('GROUP');

  const modeOptions = [
    { label: 'Групи', value: 'GROUP' },
    { label: 'Викладачі', value: 'TEACHER' },
    { label: 'Аудиторії', value: 'ROOM' },
  ];

  return (
    <div className="flex h-full overflow-hidden bg-[var(--aura-surface-color)]">
      <div className="flex-1 flex flex-col gap-4 p-4 overflow-hidden">
        <div className="flex justify-between items-center p-2 rounded-md bg-[var(--aura-surface-color)] border shadow-sm">
          <h2 className="text-xl font-bold" style={{ color: 'var(--aura-primary-text-color)' }}>
            Інтерактивний розклад
          </h2>
          <Select
            label="Режим перегляду"
            value={mode.value}
            items={modeOptions}
            onValueChanged={(e) => {
              mode.value = e.detail.value as Mode;
              selectedEntity.value = null;
            }}
          />
        </div>
        
        <div className="flex-1 overflow-hidden border rounded-md shadow-sm">
          <ScheduleGrid mode={mode.value} />
        </div>
      </div>
      
      <AnalyticsSidebar />
    </div>
  );
}
