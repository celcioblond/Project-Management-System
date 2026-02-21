// src/components/ErrorBoundary.tsx
import { Component, ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
    // Clear potentially corrupted auth data
    localStorage.removeItem('user');
    localStorage.removeItem('token');
  }

  handleReload = () => {
    this.setState({ hasError: false });
    window.location.href = '/login';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex items-center justify-center min-h-screen bg-base-200">
          <div className="text-center max-w-md p-8">
            <h1 className="text-4xl font-bold text-error mb-4">Oops!</h1>
            <p className="text-lg mb-6">
              Something went wrong. Please try again.
            </p>
            <button onClick={this.handleReload} className="btn btn-primary">
              Go to Login
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
