import React, { useState } from 'react';
import { ExternalLink, Play, AlertCircle, Video } from 'lucide-react';
import { Button } from '../common/Button';
import clsx from 'clsx';

interface YouTubePlayerProps {
  videoId?: string;
  title: string;
  className?: string;
}

export const YouTubePlayer: React.FC<YouTubePlayerProps> = ({ videoId, title, className }) => {
  const [hasError, setHasError] = useState(false);

  if (!videoId || hasError) {
    return (
      <div
        className={clsx(
          'w-full aspect-16/9 bg-slate-900 rounded-2xl flex flex-col items-center justify-center p-6 text-center text-white border border-slate-800 shadow-xl space-y-4',
          className
        )}
      >
        <div className="w-14 h-14 rounded-2xl bg-rose-500/20 text-rose-400 flex items-center justify-center border border-rose-500/30">
          <Video className="w-7 h-7" />
        </div>
        <div className="space-y-1 max-w-md">
          <h4 className="text-base font-bold">Unable to play video embed</h4>
          <p className="text-xs text-slate-400">
            This video can be watched directly on YouTube.
          </p>
        </div>
        {videoId && (
          <a
            href={`https://www.youtube.com/watch?v=${videoId}`}
            target="_blank"
            rel="noopener noreferrer"
          >
            <Button
              variant="danger"
              size="sm"
              className="font-bold text-xs gap-1.5 rounded-xl px-5"
            >
              <span>Watch on YouTube</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </Button>
          </a>
        )}
      </div>
    );
  }

  const embedUrl = `https://www.youtube.com/embed/${encodeURIComponent(videoId)}?rel=0&modestbranding=1`;

  return (
    <div className={clsx('w-full rounded-2xl sm:rounded-3xl overflow-hidden shadow-2xl bg-black border border-slate-800 relative', className)}>
      <div className="relative w-full aspect-16/9">
        <iframe
          src={embedUrl}
          title={title || 'Taxoryn Tax Video Walkthrough'}
          loading="lazy"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          allowFullScreen
          onError={() => setHasError(true)}
          className="absolute inset-0 w-full h-full border-0"
        />
      </div>
    </div>
  );
};
