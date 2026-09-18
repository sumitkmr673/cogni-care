import React from "react";
import ErrorPage from "../pages/ErrorPage";

export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    if (import.meta.env.DEV) {
      console.error("ErrorBoundary caught an unexpected rendering error:", error, errorInfo);
    }
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
    if (this.props.onReset) {
      this.props.onReset();
    }
  };

  render() {
    if (this.state.hasError) {
      const sanitizedReference = import.meta.env.DEV && this.state.error?.message
        ? this.state.error.message.replace(/([a-zA-Z0-9_\-\.]+@)/g, "").slice(0, 150)
        : undefined;

      return (
        <ErrorPage
          onRetry={this.handleReset}
          errorReference={sanitizedReference}
        />
      );
    }
    return this.props.children;
  }
}
