import React, { useState } from 'react';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { ReplacementDialog } from './ReplacementDialog';
import { isPublished } from '../store/app-state';

interface GridCellProps {
  lessons: any[]; 
  mode: 'GROUP' | 'TEACHER' | 'ROOM';
  onDragStart?: (e: React.DragEvent, id: number) => void;
  compact?: boolean;
  align?: 'left' | 'right' | 'center';
}

export const GridCell: React.FC<GridCellProps> = ({ lessons, mode, onDragStart, compact, align = 'center' }) => {
  const [activeLesson, setActiveLesson] = useState<any>(null);
  const [dialogOpened, setDialogOpened] = useState(false);
  
  if (!lessons || lessons.length === 0) return null;

  const first = lessons[0];
  const hasRealConflict = lessons.some((l: any) => l.hasConflict);
  const published = isPublished.value;

  const handleReplacement = (e: React.MouseEvent, lesson: any) => {
    e.stopPropagation();
    setActiveLesson(lesson);
    setDialogOpened(true);
  };

  const subjectFontSize = compact ? 'text-[12px]' : 'text-[16px]';
  const teacherFontSize = compact ? 'text-[11px]' : 'text-[14px]';
  const roomFontSize = compact ? 'text-[10px]' : 'text-[13px]';

  const alignClasses = align === 'left' ? 'items-start text-left' : 
                       align === 'right' ? 'items-end text-right' : 
                       'items-center text-center';

  return (
    <>
      <div 
        draggable={!published && !compact}
        onDragStart={(e) => !published && !compact && onDragStart?.(e, first.id)}
        className={`w-full p-0.5 flex flex-col justify-center relative group ${hasRealConflict ? 'bg-red-50' : 'bg-transparent'} ${alignClasses}`}
      >
        {!compact && lessons.length === 1 && first.subgroup > 0 && (
          <div className="absolute top-0 left-0 border-r border-b border-black px-1 text-[10px] font-black bg-white z-10">
            {first.subgroup}
          </div>
        )}

        <div className={`flex flex-col gap-0 w-full ${alignClasses}`}>
          {/* Subject */}
          <div className={`flex items-center gap-1 w-full relative mb-0.5 ${align === 'right' ? 'justify-end' : align === 'left' ? 'justify-start' : 'justify-center'}`}>
            <span className={`${subjectFontSize} font-black leading-tight uppercase underline decoration-1 underline-offset-2 ${hasRealConflict ? 'text-red-700' : 'text-black'}`} title={first.subjectName}>
              {first.subjectName}
            </span>
          </div>
          
          {/* Teachers */}
          <div className={`flex flex-col gap-0 w-full ${alignClasses}`}>
            {lessons.map((l) => (
              <div key={l.id} className={`relative group/teacher flex items-center w-full px-1 ${align === 'right' ? 'justify-end' : align === 'left' ? 'justify-start' : 'justify-center'}`}>
                {align === 'right' && !published && (
                   <Button 
                    theme="tertiary-inline small" 
                    className="opacity-0 group-hover/teacher:opacity-100 p-0 h-4 w-4 min-w-0 transition-opacity bg-blue-600 text-white rounded shadow-sm z-20 mr-1"
                    onClick={(e) => handleReplacement(e, l)}
                  >
                    <Icon icon="vaadin:edit" className="w-3 h-3" />
                  </Button>
                )}
                
                <span className={`${teacherFontSize} font-bold text-black leading-tight truncate`}>
                  {l.teacherName || '—'}
                </span>
                
                {(align === 'left' || align === 'center') && !published && (
                  <Button 
                    theme="tertiary-inline small" 
                    className="ml-1 opacity-0 group-hover/teacher:opacity-100 p-0 h-4 w-4 min-w-0 transition-opacity bg-blue-600 text-white rounded shadow-sm z-20"
                    onClick={(e) => handleReplacement(e, l)}
                  >
                    <Icon icon="vaadin:edit" className="w-3 h-3" />
                  </Button>
                )}
              </div>
            ))}
          </div>
          
          {/* Rooms */}
          <div className={`mt-0.5 ${roomFontSize} text-black font-bold`}>
            <span>
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
