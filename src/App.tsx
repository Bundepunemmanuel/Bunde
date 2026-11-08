import React, { useState } from 'react';
import {
  FileText,
  Upload,
  BookOpen,
  RotateCw,
  User,
  Settings,
  Sparkles,
  ChevronRight,
  CheckCircle2,
  AlertTriangle,
  Smartphone,
  Cpu,
  Code2,
  Terminal,
  Send,
  Plus,
  Trash2,
  ArrowLeft,
  MessageSquare
} from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<'home' | 'learning' | 'revision' | 'profile'>('home');
  const [activeScreen, setActiveScreen] = useState<'main' | 'upload_notes' | 'enter_topic' | 'topic_review' | 'tutor'>('main');

  const [tutorMessages, setTutorMessages] = useState([
    { id: '1', sender: 'tutor', text: 'Welcome to Privatization! Today we will explore 4 key concepts. Let\'s start with concept 1: Definition of Privatization.' },
    { id: '2', sender: 'tutor', text: 'Privatization refers to the transfer of state-owned enterprises or public assets to private business owners. The primary objective is to eliminate fiscal deficits and improve operational efficiency.' },
    { id: '3', sender: 'student', text: 'Can you give an example of a state enterprise being privatized?' },
    { id: '4', sender: 'tutor', text: 'Great question! A classic example is the privatization of state telecommunications or power distribution companies (e.g. NITEL or PHCN in Nigeria), where private investors took over management and infrastructure.' }
  ]);
  const [studentInput, setStudentInput] = useState('');
  const [currentConceptIndex, setCurrentConceptIndex] = useState(0);

  const concepts = [
    { title: 'Definition of Privatization', category: 'DEFINITION' },
    { title: 'Forms of Privatization', category: 'TYPE' },
    { title: 'Reasons for Privatization', category: 'REASON' },
    { title: 'Advantages & Disadvantages', category: 'ADVANTAGE' }
  ];

  const handleSendStudentMsg = () => {
    if (!studentInput.trim()) return;
    const newMsg = { id: Date.now().toString(), sender: 'student', text: studentInput };
    const replyMsg = {
      id: (Date.now() + 1).toString(),
      sender: 'tutor',
      text: `That's an insightful point about "${studentInput}". In this concept, understanding ownership transfer helps evaluate how market competition drives efficiency.`
    };
    setTutorMessages([...tutorMessages, newMsg, replyMsg]);
    setStudentInput('');
  };

  const handleNextConcept = () => {
    if (currentConceptIndex < concepts.length - 1) {
      const nextIdx = currentConceptIndex + 1;
      setCurrentConceptIndex(nextIdx);
      const nextMsg = {
        id: Date.now().toString(),
        sender: 'tutor',
        text: `Concept ${nextIdx + 1}: ${concepts[nextIdx].title}. Let's examine how this functions in the economy.`
      };
      setTutorMessages([...tutorMessages, nextMsg]);
    } else {
      setActiveScreen('main');
    }
  };

  return (
    <div className="flex h-screen w-full bg-[#EBE7DC] text-[#242321] overflow-hidden font-sans">
      {/* Left Sidebar: Stage 3 Interactive Tutor Highlights */}
      <div className="hidden lg:flex flex-col w-[400px] border-r border-[#D9D3C5] bg-[#FAF8F3] p-5 overflow-y-auto">
        <div className="flex items-center gap-2 mb-4">
          <div className="w-8 h-8 rounded-lg bg-[#C0584A] text-white flex items-center justify-center font-bold text-lg shadow-sm">
            B
          </div>
          <div>
            <h1 className="font-bold text-base leading-tight text-[#242321]">Bunde Stage 3</h1>
            <span className="text-xs text-[#77736D]">Interactive AI Personal Tutor & Lesson Flow</span>
          </div>
        </div>

        <div className="p-4 bg-white rounded-xl border border-[#E2DDD3] mb-4 space-y-3 shadow-xs">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-[#242321]">
              <MessageSquare className="w-4 h-4 text-[#C0584A]" />
              <span>Stage 3 Interactive Tutor</span>
            </div>
            <span className="text-[10px] px-2 py-0.5 rounded-full bg-[#E8F3EB] text-[#437A57] font-medium">
              Active
            </span>
          </div>
          <p className="text-[11px] text-[#77736D]">
            Delivers step-by-step curriculum lessons, answers student interruptions dynamically, evaluates understanding, and updates local data store.
          </p>
          <button
            onClick={() => setActiveScreen('tutor')}
            className="w-full py-2 bg-[#C0584A] text-white rounded-lg text-xs font-semibold hover:bg-[#9E3E32] transition-colors"
          >
            Launch Interactive Lesson
          </button>
        </div>

        <div className="flex-1 flex flex-col bg-white rounded-xl border border-[#E2DDD3] p-4 space-y-3">
          <div className="text-xs font-semibold text-[#242321]">Lesson State & Engine Specs</div>
          <div className="space-y-2 text-xs text-[#77736D]">
            <div className="p-2.5 bg-[#FAF8F3] rounded-lg border border-[#E2DDD3]">
              <div className="font-semibold text-[#242321] mb-0.5">1. Concept Progression</div>
              <p className="text-[11px]">Concept {currentConceptIndex + 1} of {concepts.length}: {concepts[currentConceptIndex].title}</p>
            </div>
            <div className="p-2.5 bg-[#FAF8F3] rounded-lg border border-[#E2DDD3]">
              <div className="font-semibold text-[#242321] mb-0.5">2. Low-RAM State Manager</div>
              <p className="text-[11px]">StateFlow streams conversation history without memory leak or background daemon bloat.</p>
            </div>
          </div>
        </div>
      </div>

      {/* Main Phone Mockup */}
      <div className="flex-1 flex flex-col items-center justify-center p-4 lg:p-6 overflow-y-auto">
        <div className="w-full max-w-[420px] h-[840px] max-h-[92vh] bg-[#F2EEE4] rounded-[44px] shadow-2xl border-[10px] border-[#242321] flex flex-col overflow-hidden relative">
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-36 h-5 bg-[#242321] rounded-b-2xl z-50 flex items-center justify-center">
            <div className="w-3 h-3 rounded-full bg-[#111110] border border-[#333330] mr-2" />
            <div className="w-10 h-1 rounded-full bg-[#333330]" />
          </div>

          <div className="pt-2 px-6 h-9 flex items-center justify-between text-[11px] font-medium text-[#77736D] select-none z-40 bg-[#F2EEE4]">
            <span>13:42</span>
            <div className="flex items-center gap-1.5 text-[10px]">
              <span className="font-semibold text-[#C0584A]">Stage 3 Tutor</span>
              <span>•</span>
              <span>100%</span>
            </div>
          </div>

          <div className="px-4 py-2.5 border-b border-[#E2DDD3] flex items-center justify-between bg-[#F2EEE4] z-10">
            {activeScreen === 'tutor' ? (
              <div className="flex items-center gap-2">
                <button onClick={() => setActiveScreen('main')} className="p-1 text-[#242321]">
                  <ArrowLeft className="w-5 h-5" />
                </button>
                <div>
                  <div className="font-bold text-sm text-[#242321]">Privatization</div>
                  <div className="text-[10px] text-[#77736D]">Concept {currentConceptIndex + 1} of {concepts.length}: {concepts[currentConceptIndex].title}</div>
                </div>
              </div>
            ) : (
              <div className="flex items-center gap-2.5">
                <div className="w-7 h-7 rounded-lg bg-[#C0584A] text-white flex items-center justify-center font-bold text-sm">
                  B
                </div>
                <span className="font-bold text-lg text-[#242321] tracking-tight">Bunde</span>
              </div>
            )}
          </div>

          {/* Progress Bar */}
          {activeScreen === 'tutor' && (
            <div className="w-full h-1 bg-[#E2DDD3]">
              <div
                className="h-full bg-[#C0584A] transition-all duration-300"
                style={{ width: `${((currentConceptIndex + 1) / concepts.length) * 100}%` }}
              />
            </div>
          )}

          {/* Content Area */}
          <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
            {activeScreen === 'tutor' ? (
              <>
                {tutorMessages.map((msg) => (
                  <div
                    key={msg.id}
                    className={`flex ${msg.sender === 'student' ? 'justify-end' : 'justify-start'} gap-2`}
                  >
                    {msg.sender === 'tutor' && (
                      <div className="w-7 h-7 rounded-lg bg-[#C0584A] text-white flex items-center justify-center font-bold text-xs flex-shrink-0 mt-0.5">
                        B
                      </div>
                    )}
                    <div
                      className={`max-w-[80%] p-3 text-xs leading-relaxed ${
                        msg.sender === 'student'
                          ? 'bg-[#C0584A] text-white rounded-2xl rounded-tr-xs'
                          : 'bg-white text-[#242321] border border-[#E2DDD3] rounded-2xl rounded-tl-xs shadow-xs'
                      }`}
                    >
                      {msg.text}
                    </div>
                  </div>
                ))}

                {/* Concept Check Evaluation Card */}
                <div className="p-3.5 bg-[#E8F3EB] border border-[#437A57]/30 rounded-xl space-y-2">
                  <div className="flex items-center gap-1.5 text-xs font-bold text-[#437A57]">
                    <CheckCircle2 className="w-4 h-4" />
                    <span>Concept Evaluation • Score 9/10</span>
                  </div>
                  <p className="text-[11px] text-[#77736D]">
                    Great understanding! You clearly identified ownership transfer and market efficiency.
                  </p>
                  <button
                    onClick={handleNextConcept}
                    className="w-full py-2 bg-[#C0584A] text-white rounded-lg text-xs font-semibold shadow-xs"
                  >
                    {currentConceptIndex < concepts.length - 1 ? 'Next Concept' : 'Finish Lesson'}
                  </button>
                </div>
              </>
            ) : (
              <div className="space-y-3">
                <div className="text-xs font-semibold uppercase tracking-wider text-[#C0584A]">Bunde</div>
                <h2 className="text-2xl font-bold text-[#242321]">What are we learning today?</h2>
                <div
                  onClick={() => setActiveScreen('tutor')}
                  className="p-4 bg-white rounded-xl border border-[#E2DDD3] cursor-pointer hover:border-[#C0584A]/50 transition-all shadow-xs"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-[#F8EAE8] text-[#C0584A] flex items-center justify-center font-bold">
                      B
                    </div>
                    <div className="flex-1">
                      <div className="font-semibold text-sm text-[#242321]">Economics • Privatization</div>
                      <div className="text-xs text-[#77736D]">Tap to launch Interactive AI Lesson</div>
                    </div>
                    <ChevronRight className="w-4 h-4 text-[#77736D]" />
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Student Question Bar */}
          {activeScreen === 'tutor' && (
            <div className="p-3 bg-white border-t border-[#E2DDD3] flex items-center gap-2">
              <input
                type="text"
                placeholder="Ask Bunde anything about this lesson..."
                value={studentInput}
                onChange={(e) => setStudentInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSendStudentMsg()}
                className="flex-1 px-3 py-2 bg-[#FAF8F3] border border-[#E2DDD3] rounded-full text-xs outline-none"
              />
              <button
                onClick={handleSendStudentMsg}
                className="w-8 h-8 rounded-full bg-[#C0584A] text-white flex items-center justify-center flex-shrink-0"
              >
                <Send className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          <div className="h-12 border-t border-[#E2DDD3] bg-[#F2EEE4] px-4 flex items-center justify-around z-20">
            <button onClick={() => setActiveScreen('main')} className="flex flex-col items-center gap-1 text-[#C0584A]">
              <FileText className="w-4 h-4" />
              <span className="text-[10px] font-medium">Home</span>
            </button>
            <button onClick={() => setActiveScreen('tutor')} className="flex flex-col items-center gap-1 text-[#77736D]">
              <BookOpen className="w-4 h-4" />
              <span className="text-[10px] font-medium">Tutor</span>
            </button>
            <button className="flex flex-col items-center gap-1 text-[#77736D]">
              <RotateCw className="w-4 h-4" />
              <span className="text-[10px] font-medium">Revision</span>
            </button>
            <button className="flex flex-col items-center gap-1 text-[#77736D]">
              <User className="w-4 h-4" />
              <span className="text-[10px] font-medium">Profile</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
