import React, { useState } from 'react';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { ReplacementDialog } from './ReplacementDialog';
import { isPublished } from '../store/app-state';

interface GridCellProps {
  lessons: any[]; 
  mode: 'GROUP' | 'TEACHER' | 'ROOM';
  onDragStart?: (e: React.DragEvent, id: number) => void;
}

export const GridCell: React.FC<GridCellProps> = ({ lessons, mode, onDragStart }) => {
  const [activeLesson, setActiveLesson] = useState<any>(null);
  const [dialogOpened, setDialogOpened] = useState(false);
  
  if (!lessons || lessons.length === 0) return null;

  const first = lessons[0];
  // Клітинка червона лише якщо є РЕАЛЬНИЙ конфлікт (крім поділу на підгрупи)
  const hasRealConflict = lessons.some((l: any) => l.hasConflict);
  const published = isPublished.value;

  const handleReplacement = (e: React.MouseEvent, lesson: any) => {
    e.stopPropagation();
    setActiveLesson(lesson);
    setDialogOpened(true);
  };

  return (
    <>
      <div 
        draggable={!published}
        onDragStart={(e) => onDragStart?.(e, first.id)}
        className={`h-full w-full p-1 flex flex-col justify-center items-center text-center relative group ${hasRealConflict ? 'bg-red-50' : 'bg-transparent'}`}
      >
        <div className="flex flex-col gap-0 items-center justify-center w-full overflow-hidden">
          <div className="flex items-center justify-center gap-1 w-full relative mb-1">
            <span className={`text-[16px] font-black leading-tight uppercase underline decoration-2 underline-offset-2 ${hasRealConflict ? 'text-red-700' : 'text-black'}`} title={first.subjectName}>
              {first.subjectName}
            </span>
          </div>
          
          <div className="flex flex-col gap-0 items-center w-full">
            {lessons.map((l) => (
              <div key={l.id} className="relative group/teacher flex items-center justify-center w-full">
                <span className="text-[14px] font-bold text-black leading-tight">
                  {l.teacherName || '—'}
                </span>
                {!published && (
                  <Button 
                    theme="tertiary-inline small" 
                    className="absolute -right-2 opacity-0 group-hover/teacher:opacity-100 p-0 h-4 w-4 min-w-0 transition-opacity bg-white/80 rounded-full shadow-sm"
                    onClick={(e) => handleReplacement(e, l)}
                    title="Замінити викладача/аудиторію"
                  >
                    <Icon icon="vaadin:edit" className="w-3 h-3 text-blue-600" />
                  </Button>
                )}
              </div>
            ))}
          </div>
          
          <div className="mt-1 text-[13px] text-black font-bold">
            <span className="font-bold">
              ауд.№{lessons.map(l => l.roomName || '—').filter((v, i, a) => a.indexOf(v) === i).join(', ')}
            </span>
          </div>
        </div>
        
        {hasRealConflict && (
          <div className="absolute top-0 right-0 -mt-1 -mr-1">
             <span className="flex h-3 w-3 relative">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500 text-[8px] text-white items-center justify-center font-bold">!</span>
            </span>
          </div>
        )}
      </div>

      {activeLesson && (
        <ReplacementDialog 
          lesson={activeLesson} 
          opened={dialogOpened} 
          onClose={() => {
            setDialogOpened(false);
            setActiveLesson(null);
          }} 
        />
      )}
    </>
  );
};
