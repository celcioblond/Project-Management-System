import { useEffect, useState, useCallback, useRef } from 'react';
import { apiService } from '../../services/api';
import { useAuth } from '../../context/AuthContext';

interface ChatMessage {
  id: number;
  text: string;
  sender: 'user' | 'bot';
  timestamp: Date;
}

const WELCOME_MESSAGE: ChatMessage = {
  id: 0,
  text: "Hi! I'm your Project Assistant 🤖\n\nI can help you:\n• Review your projects & deadlines\n• Analyze task workload\n• Recommend next actions\n• Answer questions about your work\n\nWhat would you like to know?",
  sender: 'bot',
  timestamp: new Date(),
};

const SUGGESTION_CHIPS = [
  'Summarize my projects',
  "What's due soon?",
  'My workload this week',
  'Action plan for today',
];

export default function AiChatBot() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([WELCOME_MESSAGE]);
  const [inputValue, setInputValue] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hasUnread, setHasUnread] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const idCounter = useRef(1);
  const { user } = useAuth();

  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isTyping]);

  useEffect(() => {
    if (isOpen) {
      setHasUnread(false);
      setTimeout(() => inputRef.current?.focus(), 150);
    }
  }, [isOpen]);

  const sendMessage = useCallback(
    async (text: string) => {
      if (!text.trim() || isTyping) return;

      const userMsg: ChatMessage = {
        id: idCounter.current++,
        text: text.trim(),
        sender: 'user',
        timestamp: new Date(),
      };

      setMessages((prev) => [...prev, userMsg]);
      setInputValue('');
      setIsTyping(true);
      setError(null);

      try {
        const botText = await apiService.askBot(
          text.trim(),
          user?.username ?? '',
        );

        const botMsg: ChatMessage = {
          id: idCounter.current++,
          text: botText,
          sender: 'bot',
          timestamp: new Date(),
        };

        setMessages((prev) => [...prev, botMsg]);
        if (!isOpen) setHasUnread(true);
      } catch (err: any) {
        setError(err.message || 'Something went wrong.');
      } finally {
        setIsTyping(false);
      }
    },
    [isTyping, isOpen],
  );

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage(inputValue);
    }
  };

  const formatTime = (d: Date) =>
    d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  const formatText = (text: string) =>
    text.split('\n').map((line, i) => (
      <span key={i}>
        {line}
        {i < text.split('\n').length - 1 && <br />}
      </span>
    ));

  return (
    <>
      {/* Floating toggle button */}
      <button
        onClick={() => setIsOpen((o) => !o)}
        style={{
          position: 'fixed',
          bottom: '28px',
          right: '28px',
          width: '56px',
          height: '56px',
          borderRadius: '50%',
          background: isOpen
            ? '#374151'
            : 'linear-gradient(135deg, #374151 0%, #1f2937 100%)',
          border: 'none',
          cursor: 'pointer',
          boxShadow: isOpen
            ? '0 4px 20px rgba(55,65,81,0.35)'
            : '0 6px 24px rgba(55,65,81,0.4)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          transition: 'all 0.25s cubic-bezier(0.34,1.56,0.64,1)',
          transform: isOpen
            ? 'rotate(0deg) scale(0.95)'
            : 'rotate(0deg) scale(1)',
          zIndex: 1000,
        }}
        aria-label={isOpen ? 'Close assistant' : 'Open AI assistant'}
      >
        {/* Unread badge */}
        {hasUnread && !isOpen && (
          <span
            style={{
              position: 'absolute',
              top: '6px',
              right: '6px',
              width: '10px',
              height: '10px',
              borderRadius: '50%',
              background: '#ef4444',
              border: '2px solid white',
            }}
          />
        )}

        {/* Icon: bot when closed, X when open */}
        {isOpen ? (
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="white"
            strokeWidth="2.5"
            strokeLinecap="round"
          >
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        ) : (
          <svg
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="white"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <rect x="3" y="11" width="18" height="10" rx="2" />
            <path d="M9 11V7a3 3 0 0 1 6 0v4" />
            <circle cx="9" cy="16" r="1" fill="white" />
            <circle cx="15" cy="16" r="1" fill="white" />
            <path d="M12 3v2" />
          </svg>
        )}
      </button>

      {/* Chat panel */}
      <div
        style={{
          position: 'fixed',
          bottom: '96px',
          right: '28px',
          width: '380px',
          height: '560px',
          background: '#f5f4f0',
          borderRadius: '16px',
          boxShadow:
            '0 20px 60px rgba(0,0,0,0.15), 0 4px 16px rgba(0,0,0,0.08)',
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          zIndex: 999,
          opacity: isOpen ? 1 : 0,
          transform: isOpen
            ? 'translateY(0) scale(1)'
            : 'translateY(16px) scale(0.96)',
          pointerEvents: isOpen ? 'all' : 'none',
          transition:
            'opacity 0.22s ease, transform 0.22s cubic-bezier(0.34,1.2,0.64,1)',
          fontFamily: 'Georgia, serif',
        }}
      >
        {/* Header */}
        <div
          style={{
            padding: '16px 20px',
            background: '#2d3748',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            flexShrink: 0,
          }}
        >
          <div
            style={{
              width: '36px',
              height: '36px',
              borderRadius: '50%',
              background: 'rgba(255,255,255,0.12)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="white"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <rect x="3" y="11" width="18" height="10" rx="2" />
              <path d="M9 11V7a3 3 0 0 1 6 0v4" />
              <circle cx="9" cy="16" r="1" fill="white" />
              <circle cx="15" cy="16" r="1" fill="white" />
            </svg>
          </div>
          <div>
            <div
              style={{
                color: 'white',
                fontWeight: '600',
                fontSize: '14px',
                letterSpacing: '0.02em',
              }}
            >
              Project Assistant
            </div>
            <div
              style={{
                color: 'rgba(255,255,255,0.55)',
                fontSize: '12px',
                display: 'flex',
                alignItems: 'center',
                gap: '5px',
              }}
            >
              <span
                style={{
                  width: '6px',
                  height: '6px',
                  borderRadius: '50%',
                  background: '#4ade80',
                  display: 'inline-block',
                }}
              />
              Online
            </div>
          </div>
          <button
            onClick={() => setIsOpen(false)}
            style={{
              marginLeft: 'auto',
              background: 'none',
              border: 'none',
              color: 'rgba(255,255,255,0.5)',
              cursor: 'pointer',
              padding: '4px',
              borderRadius: '6px',
              display: 'flex',
              transition: 'color 0.15s',
            }}
            onMouseEnter={(e) => (e.currentTarget.style.color = 'white')}
            onMouseLeave={(e) =>
              (e.currentTarget.style.color = 'rgba(255,255,255,0.5)')
            }
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
            >
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        {/* Messages */}
        <div
          style={{
            flex: 1,
            overflowY: 'auto',
            padding: '16px',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
            scrollbarWidth: 'thin',
            scrollbarColor: '#d1cfc9 transparent',
          }}
        >
          {messages.map((msg) => (
            <div
              key={msg.id}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: msg.sender === 'user' ? 'flex-end' : 'flex-start',
                gap: '4px',
              }}
            >
              <div
                style={{
                  maxWidth: '85%',
                  padding: '10px 14px',
                  borderRadius:
                    msg.sender === 'user'
                      ? '16px 16px 4px 16px'
                      : '16px 16px 16px 4px',
                  background: msg.sender === 'user' ? '#2d3748' : '#e8e6e0',
                  color: msg.sender === 'user' ? 'white' : '#2d3748',
                  fontSize: '13.5px',
                  lineHeight: '1.55',
                  boxShadow:
                    msg.sender === 'user'
                      ? '0 2px 8px rgba(45,55,72,0.25)'
                      : '0 1px 4px rgba(0,0,0,0.08)',
                }}
              >
                {formatText(msg.text)}
              </div>
              <span
                style={{
                  fontSize: '11px',
                  color: '#9ca3af',
                  padding: '0 4px',
                }}
              >
                {formatTime(msg.timestamp)}
              </span>
            </div>
          ))}

          {/* Typing indicator */}
          {isTyping && (
            <div
              style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}
            >
              <div
                style={{
                  padding: '10px 16px',
                  borderRadius: '16px 16px 16px 4px',
                  background: '#e8e6e0',
                  display: 'flex',
                  gap: '4px',
                  alignItems: 'center',
                }}
              >
                {[0, 1, 2].map((i) => (
                  <span
                    key={i}
                    style={{
                      width: '6px',
                      height: '6px',
                      borderRadius: '50%',
                      background: '#9ca3af',
                      display: 'inline-block',
                      animation: 'bounce 1.2s ease infinite',
                      animationDelay: `${i * 0.2}s`,
                    }}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Error */}
          {error && (
            <div
              style={{
                padding: '10px 14px',
                borderRadius: '10px',
                background: '#fef2f2',
                border: '1px solid #fecaca',
                color: '#dc2626',
                fontSize: '13px',
                display: 'flex',
                gap: '8px',
                alignItems: 'flex-start',
              }}
            >
              <svg
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                style={{ flexShrink: 0, marginTop: '1px' }}
              >
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              {error}
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Suggestion chips — shown only at start */}
        {messages.length === 1 && (
          <div
            style={{
              padding: '0 16px 10px',
              display: 'flex',
              flexWrap: 'wrap',
              gap: '6px',
              flexShrink: 0,
            }}
          >
            {SUGGESTION_CHIPS.map((chip) => (
              <button
                key={chip}
                onClick={() => sendMessage(chip)}
                style={{
                  padding: '6px 12px',
                  borderRadius: '20px',
                  border: '1px solid #d1cfc9',
                  background: 'white',
                  color: '#374151',
                  fontSize: '12px',
                  cursor: 'pointer',
                  transition: 'all 0.15s',
                  fontFamily: 'inherit',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#2d3748';
                  e.currentTarget.style.color = 'white';
                  e.currentTarget.style.borderColor = '#2d3748';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'white';
                  e.currentTarget.style.color = '#374151';
                  e.currentTarget.style.borderColor = '#d1cfc9';
                }}
              >
                {chip}
              </button>
            ))}
          </div>
        )}

        {/* Input area */}
        <div
          style={{
            padding: '12px 16px',
            borderTop: '1px solid #e0ddd6',
            background: '#f5f4f0',
            flexShrink: 0,
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'flex-end',
              gap: '8px',
              background: 'white',
              borderRadius: '12px',
              border: '1.5px solid #d1cfc9',
              padding: '8px 8px 8px 14px',
              transition: 'border-color 0.15s',
            }}
            onFocusCapture={(e) =>
              (e.currentTarget.style.borderColor = '#2d3748')
            }
            onBlurCapture={(e) =>
              (e.currentTarget.style.borderColor = '#d1cfc9')
            }
          >
            <textarea
              ref={inputRef}
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask me anything…"
              rows={1}
              disabled={isTyping}
              style={{
                flex: 1,
                border: 'none',
                outline: 'none',
                resize: 'none',
                background: 'transparent',
                fontSize: '13.5px',
                lineHeight: '1.5',
                color: '#1f2937',
                fontFamily: 'inherit',
                maxHeight: '80px',
                overflowY: 'auto',
              }}
            />
            <button
              onClick={() => sendMessage(inputValue)}
              disabled={!inputValue.trim() || isTyping}
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '8px',
                border: 'none',
                background:
                  inputValue.trim() && !isTyping ? '#2d3748' : '#e5e3de',
                color: inputValue.trim() && !isTyping ? 'white' : '#9ca3af',
                cursor:
                  inputValue.trim() && !isTyping ? 'pointer' : 'not-allowed',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
                transition: 'all 0.15s',
              }}
            >
              <svg
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <line x1="22" y1="2" x2="11" y2="13" />
                <polygon points="22 2 15 22 11 13 2 9 22 2" />
              </svg>
            </button>
          </div>
          <div
            style={{
              textAlign: 'center',
              marginTop: '8px',
              fontSize: '11px',
              color: '#b0ada6',
            }}
          >
            Enter to send · Shift+Enter for new line
          </div>
        </div>
      </div>

      {/* Bounce animation */}
      <style>{`
        @keyframes bounce {
          0%, 60%, 100% { transform: translateY(0); }
          30% { transform: translateY(-4px); }
        }
      `}</style>
    </>
  );
}
