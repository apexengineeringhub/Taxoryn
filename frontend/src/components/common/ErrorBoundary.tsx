import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertOctagon, RefreshCw, Home, ArrowLeft } from 'lucide-react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
  fallbackTitle?: string;
  onReset?: () => void;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
    errorInfo: null,
  };

  public static getDerivedStateFromError(error: Error): Partial<State> {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error caught by ErrorBoundary:', error, errorInfo);
    this.setState({ errorInfo });
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
    if (this.props.onReset) {
      this.props.onReset();
    }
  };

  public render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="min-h-[50vh] flex flex-col items-center justify-center p-6 text-center animate-fade-in select-none">
          <div className="w-16 h-16 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center mb-4 shadow-xs border border-rose-200">
            <AlertOctagon className="w-8 h-8" />
          </div>
          <h2 className="text-xl font-black text-slate-900 tracking-tight">
            {this.props.fallbackTitle || 'Something went wrong on this page'}
          </h2>
          <p className="text-xs text-slate-500 max-w-md mt-2 mb-6 leading-relaxed">
            An unexpected error occurred while rendering this view. You can reload the component or return to your workspace dashboard.
          </p>

          <div className="flex flex-wrap items-center justify-center gap-3">
            <button
              onClick={() => {
                if (window.history.length > 1) {
                  window.history.back();
                } else {
                  window.location.href = '/dashboard';
                }
              }}
              className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-xl text-xs transition-colors inline-flex items-center gap-1.5"
            >
              <ArrowLeft className="w-4 h-4" /> Go Back
            </button>
            <button
              onClick={this.handleReset}
              className="px-3 py-2 bg-brand-600 hover:bg-brand-700 text-white font-bold rounded-xl text-xs transition-colors inline-flex items-center gap-1.5 shadow-xs"
            >
              <RefreshCw className="w-4 h-4" /> Try Again
            </button>
            <button
              onClick={() => {
                window.location.href = '/dashboard';
              }}
              className="px-3 py-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 font-bold rounded-xl text-xs transition-colors inline-flex items-center gap-1.5 shadow-2xs"
            >
              <Home className="w-4 h-4" /> Return to Dashboard
            </button>
          </div>

          {this.state.error && (
            <details className="mt-6 text-left max-w-lg w-full bg-slate-100 p-3 rounded-xl border border-slate-200 text-[11px] font-mono text-slate-600 overflow-auto max-h-48">
              <summary className="cursor-pointer font-bold text-rose-600 mb-1">
                Technical Details ({this.state.error.name}: {this.state.error.message})
              </summary>
              <pre className="whitespace-pre-wrap text-[10px] text-slate-500">
                {this.state.error.stack}
              </pre>
            </details>
          )}
        </div>
      );
    }

    return this.props.children;
  }
}
