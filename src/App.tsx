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
  Layers
} from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<'home' | 'learning' | 'revision' | 'profile'>('home');
  const [activeScreen, setActiveScreen] = useState<'main' | 'upload_notes' | 'enter_topic' | 'topic_review'>('main');
  
  // Stage 2 state simulation
  const [noteImages, setNoteImages] = useState<string[]>([
    'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&auto=format&fit=crop&q=60',
    'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=300&auto=format&fit=crop&q=60'
  ]);
  const [inputSubject, setInputSubject] = useState('Economics');
  const [inputTopic, setInputTopic] = useState('Privatization');
  const [inputLevel, setInputLevel] = useState('SS2');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [gitStatus, setGitStatus] = useState<'idle' | 'pushing' | 'success'>('idle');

  const generatedTopicMap = {
    topicName: inputTopic || 'Privatization',
    subject: inputSubject || 'Economics',
    classLevel: inputLevel || 'SS2',
    summary: 'Privatization involves the transfer of ownership of state-owned enterprises to private investors to improve operational efficiency and reduce public sector borrowing.',
    unreadableNoteNotice: null,
    subtopics: [
      { id: '1', title: 'Definition of Privatization', category: 'DEFINITION', shortSummary: 'Transfer of state assets to private entities' },
      { id: '2', title: 'Forms of Privatization', category: 'TYPE', shortSummary: 'Share sales, asset sales, and concessions' },
      { id: '3', title: 'Reasons for Privatization', category: 'REASON', shortSummary: 'Eliminating government fiscal deficits and inefficiency' },
      { id: '4', title: 'Advantages & Disadvantages', category: 'ADVANTAGE', shortSummary: 'Better productivity vs potential job losses or monopoly pricing' }
    ]
  };

  const sampleTopics = [
    {
      id: 'topic_privatization',
      subject: 'Economics',
      name: 'Privatization',
      level: 'SS2',
      progress: 65,
      completed: 4,
      total: 7
    },
    {
      id: 'topic_cell_division',
      subject: 'Biology',
      name: 'Mitosis and Meiosis',
      level: 'SS2',
      progress: 90,
      completed: 5,
      total: 6
    }
  ];

  const handlePushToGithub = () => {
    setGitStatus('pushing');
    setTimeout(() => {
      setGitStatus('success');
    }, 1500);
  };

  return (
    <div className="flex h-screen w-full bg-[#EBE7DC] text-[#242321] overflow-hidden font-sans">
      {/* Left Sidebar: GitHub Commit & Stage 2 Inspector */}
      <div className="hidden lg:flex flex-col w-[400px] border-r border-[#D9D3C5] bg-[#FAF8F3] p-5 overflow-y-auto">
        <div className="flex items-center gap-2 mb-4">
          <div className="w-8 h-8 rounded-lg bg-[#C0584A] text-white flex items-center justify-center font-bold text-lg shadow-sm">
            B
          </div>
          <div>
            <h1 className="font-bold text-base leading-tight text-[#242321]">Bunde Stage 2</h1>
            <span className="text-xs text-[#77736D]">Topic Input & Gemini Multimodal Analysis</span>
          </div>
        </div>

        {/* GitHub Repository Push Card */}
        <div className="p-4 bg-white rounded-xl border border-[#E2DDD3] mb-4 space-y-3 shadow-xs">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-[#242321]">
              <Terminal className="w-4 h-4 text-[#C0584A]" />
              <span>GitHub Repo Commit & Push</span>
            </div>
            <span className="text-[10px] px-2 py-0.5 rounded-full bg-[#E8F3EB] text-[#437A57] font-medium">
              Connected
            </span>
          </div>
          <p className="text-[11px] text-[#77736D] font-mono bg-[#F2EEE4] p-2 rounded break-all">
            https://github.com/Bundepunemmanuel/Bund
          </p>
          <div className="text-[11px] text-[#77736D] space-y-1">
            <div>• Token: <code className="text-[#C0584A]">ghp_exiABgo61...</code></div>
            <div>• Unified Signing Key: <code className="text-[#242321]">bunde-debug.keystore</code></div>
          </div>
          <button
            onClick={handlePushToGithub}
            disabled={gitStatus === 'pushing'}
            className="w-full py-2 bg-[#242321] text-white rounded-lg text-xs font-semibold hover:bg-[#383531] transition-colors flex items-center justify-center gap-2"
          >
            <Send className="w-3.5 h-3.5" />
            {gitStatus === 'idle' ? 'Commit & Push Code to Repo' : gitStatus === 'pushing' ? 'Pushing to GitHub...' : 'Successfully Pushed!'}
          </button>
          {gitStatus === 'success' && (
            <div className="text-[11px] text-[#437A57] flex items-center gap-1 justify-center">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>All Stage 2 Kotlin sources & Gradle configs committed</span>
            </div>
          )}
        </div>

        {/* Stage 2 Features Overview */}
        <div className="flex-1 flex flex-col bg-white rounded-xl border border-[#E2DDD3] p-4 space-y-3">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-[#242321]">
            <Sparkles className="w-4 h-4 text-[#C0584A]" />
            <span>Stage 2 Architecture Highlights</span>
          </div>
          <div className="space-y-2 text-xs text-[#77736D]">
            <div className="p-2.5 bg-[#FAF8F3] rounded-lg border border-[#E2DDD3]">
              <div className="font-semibold text-[#242321] mb-0.5">1. Native Image Picker & Compression</div>
              <p className="text-[11px]">Multi-image selection with preview grid, thumbnail deletion, and 1024px RGB_565 compression for 2GB RAM devices.</p>
            </div>
            <div className="p-2.5 bg-[#FAF8F3] rounded-lg border border-[#E2DDD3]">
              <div className="font-semibold text-[#242321] mb-0.5">2. Gemini Multimodal Analysis</div>
              <p className="text-[11px]">Sends compressed image bytes via OkHttp REST to gemini-1.5-flash with structured JSON response parsing.</p>
            </div>
            <div className="p-2.5 bg-[#FAF8F3] rounded-lg border border-[#E2DDD3]">
              <div className="font-semibold text-[#242321] mb-0.5">3. Structured Topic Maps</div>
              <p className="text-[11px]">Categorized subtopics (Definitions, Types, Reasons, Advantages) saved locally via DataStore and JSON files.</p>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content Area: Phone Mockup Container */}
      <div className="flex-1 flex flex-col items-center justify-center p-4 lg:p-6 overflow-y-auto">
        <div className="w-full max-w-[420px] h-[840px] max-h-[92vh] bg-[#F2EEE4] rounded-[44px] shadow-2xl border-[10px] border-[#242321] flex flex-col overflow-hidden relative">
          {/* Phone Speaker & Camera Notch */}
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-36 h-5 bg-[#242321] rounded-b-2xl z-50 flex items-center justify-center">
            <div className="w-3 h-3 rounded-full bg-[#111110] border border-[#333330] mr-2" />
            <div className="w-10 h-1 rounded-full bg-[#333330]" />
          </div>

          {/* Android Status Bar */}
          <div className="pt-2 px-6 h-9 flex items-center justify-between text-[11px] font-medium text-[#77736D] select-none z-40 bg-[#F2EEE4]">
            <span>13:41</span>
            <div className="flex items-center gap-1.5 text-[10px]">
              <span className="font-semibold text-[#C0584A]">Stage 2 Active</span>
              <span>•</span>
              <span>2GB RAM</span>
            </div>
          </div>

          {/* Top Bar */}
          <div className="px-5 py-3 border-b border-[#E2DDD3] flex items-center justify-between bg-[#F2EEE4] z-10">
            <div className="flex items-center gap-2.5">
              <div className="w-7 h-7 rounded-lg bg-[#C0584A] text-white flex items-center justify-center font-bold text-sm">
                B
              </div>
              <span className="font-bold text-lg text-[#242321] tracking-tight">Bunde</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-white border border-[#E2DDD3] text-[10px] font-medium text-[#77736D]">
                <span className="w-1.5 h-1.5 rounded-full bg-[#437A57]"></span>
                Connected
              </span>
              <button
                onClick={() => setActiveTab('profile')}
                className="w-8 h-8 rounded-full flex items-center justify-center text-[#77736D] hover:bg-black/5"
              >
                <Settings className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Scrollable Screen Content */}
          <div className="flex-1 overflow-y-auto px-5 py-4 space-y-4">
            {activeScreen === 'upload_notes' ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-bold text-[#242321]">Upload School Notes</h2>
                  <button onClick={() => setActiveScreen('main')} className="text-xs text-[#C0584A] font-medium">Cancel</button>
                </div>
                <p className="text-xs text-[#77736D]">
                  Select note photos. Gemini extracts headings, definitions, and concepts.
                </p>

                <input
                  type="text"
                  placeholder="Subject (e.g. Economics, Biology)"
                  value={inputSubject}
                  onChange={(e) => setInputSubject(e.target.value)}
                  className="w-full p-2.5 bg-white border border-[#E2DDD3] rounded-lg text-xs"
                />

                <div className="p-3 bg-white border border-[#E2DDD3] rounded-xl space-y-2">
                  <div className="flex justify-between items-center text-xs font-semibold">
                    <span>Note Photos Preview ({noteImages.length})</span>
                    <button
                      onClick={() => setNoteImages([...noteImages, 'https://images.unsplash.com/photo-1501504905252-473c47e087f8?w=300&auto=format&fit=crop&q=60'])}
                      className="text-[#C0584A] flex items-center gap-1 text-[11px]"
                    >
                      <Plus className="w-3.5 h-3.5" /> Add Photo
                    </button>
                  </div>
                  <div className="grid grid-cols-3 gap-2">
                    {noteImages.map((img, idx) => (
                      <div key={idx} className="relative h-20 rounded-lg overflow-hidden border border-[#E2DDD3]">
                        <img src={img} alt="Note" className="w-full h-full object-cover" />
                        <button
                          onClick={() => setNoteImages(noteImages.filter((_, i) => i !== idx))}
                          className="absolute top-1 right-1 w-5 h-5 bg-[#C0584A] text-white rounded-full flex items-center justify-center text-[10px]"
                        >
                          ✕
                        </button>
                      </div>
                    ))}
                  </div>
                </div>

                <button
                  onClick={() => {
                    setIsAnalyzing(true);
                    setTimeout(() => {
                      setIsAnalyzing(false);
                      setActiveScreen('topic_review');
                    }, 1500);
                  }}
                  disabled={isAnalyzing}
                  className="w-full py-3 bg-[#C0584A] text-white rounded-xl font-semibold text-sm shadow-sm"
                >
                  {isAnalyzing ? 'Analyzing Notes with Gemini...' : 'Analyze Notes & Build Topic Map'}
                </button>
              </div>
            ) : activeScreen === 'enter_topic' ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-bold text-[#242321]">Enter a Topic</h2>
                  <button onClick={() => setActiveScreen('main')} className="text-xs text-[#C0584A] font-medium">Cancel</button>
                </div>
                <p className="text-xs text-[#77736D]">
                  Enter subject and topic for Gemini to build a complete curriculum map.
                </p>

                <div className="space-y-3">
                  <div>
                    <label className="text-[11px] font-semibold text-[#77736D]">Subject</label>
                    <input
                      type="text"
                      value={inputSubject}
                      onChange={(e) => setInputSubject(e.target.value)}
                      className="w-full p-2.5 bg-white border border-[#E2DDD3] rounded-lg text-xs mt-1"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] font-semibold text-[#77736D]">Topic Name</label>
                    <input
                      type="text"
                      value={inputTopic}
                      onChange={(e) => setInputTopic(e.target.value)}
                      className="w-full p-2.5 bg-white border border-[#E2DDD3] rounded-lg text-xs mt-1"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] font-semibold text-[#77736D]">Class Level</label>
                    <input
                      type="text"
                      value={inputLevel}
                      onChange={(e) => setInputLevel(e.target.value)}
                      className="w-full p-2.5 bg-white border border-[#E2DDD3] rounded-lg text-xs mt-1"
                    />
                  </div>
                </div>

                <button
                  onClick={() => {
                    setIsAnalyzing(true);
                    setTimeout(() => {
                      setIsAnalyzing(false);
                      setActiveScreen('topic_review');
                    }, 1500);
                  }}
                  disabled={isAnalyzing}
                  className="w-full py-3 bg-[#C0584A] text-white rounded-xl font-semibold text-sm shadow-sm"
                >
                  {isAnalyzing ? 'Generating Topic Map...' : 'Generate Structured Topic Map'}
                </button>
              </div>
            ) : activeScreen === 'topic_review' ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-bold text-[#242321]">Topic Map Review</h2>
                  <button onClick={() => setActiveScreen('main')} className="text-xs text-[#C0584A] font-medium">Done</button>
                </div>

                <div className="p-4 bg-white rounded-xl border border-[#E2DDD3] space-y-2">
                  <div className="text-[10px] font-bold text-[#C0584A] uppercase tracking-wide">
                    {generatedTopicMap.subject} • {generatedTopicMap.classLevel}
                  </div>
                  <div className="text-lg font-bold text-[#242321]">{generatedTopicMap.topicName}</div>
                  <p className="text-xs text-[#77736D] leading-relaxed">{generatedTopicMap.summary}</p>
                </div>

                <div className="space-y-2">
                  <div className="text-xs font-semibold text-[#242321]">Curriculum Components ({generatedTopicMap.subtopics.length} subtopics)</div>
                  {generatedTopicMap.subtopics.map((sub) => (
                    <div key={sub.id} className="p-3 bg-white rounded-xl border border-[#E2DDD3]">
                      <div className="flex justify-between items-center mb-1">
                        <span className="text-[10px] font-bold text-[#2C5E7A]">{sub.category}</span>
                      </div>
                      <div className="text-sm font-semibold text-[#242321]">{sub.title}</div>
                      <div className="text-xs text-[#77736D] mt-0.5">{sub.shortSummary}</div>
                    </div>
                  ))}
                </div>

                <button
                  onClick={() => setActiveScreen('main')}
                  className="w-full py-3 bg-[#C0584A] text-white rounded-xl font-semibold text-sm"
                >
                  Start Interactive Lesson
                </button>
              </div>
            ) : activeTab === 'home' ? (
              <>
                <div>
                  <div className="text-xs font-semibold uppercase tracking-wider text-[#C0584A] mb-0.5">Bunde</div>
                  <h2 className="text-2xl font-bold text-[#242321] tracking-tight">What are we learning today?</h2>
                  <p className="text-xs text-[#77736D] mt-0.5">Class: SS2 • Stage 2 Active</p>
                </div>

                <div className="space-y-3">
                  <div
                    onClick={() => setActiveScreen('upload_notes')}
                    className="p-4 bg-white rounded-xl border border-[#E2DDD3] cursor-pointer hover:border-[#C0584A]/50 transition-all shadow-xs"
                  >
                    <div className="flex items-center gap-3.5">
                      <div className="w-11 h-11 rounded-xl bg-[#F8EAE8] text-[#C0584A] flex items-center justify-center flex-shrink-0">
                        <Upload className="w-5 h-5" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="font-semibold text-sm text-[#242321]">Upload Notes</div>
                        <div className="text-xs text-[#77736D] leading-relaxed">
                          Scan notes for Gemini multimodal analysis.
                        </div>
                      </div>
                      <ChevronRight className="w-4 h-4 text-[#77736D] flex-shrink-0" />
                    </div>
                  </div>

                  <div
                    onClick={() => setActiveScreen('enter_topic')}
                    className="p-4 bg-white rounded-xl border border-[#E2DDD3] cursor-pointer hover:border-[#C0584A]/50 transition-all shadow-xs"
                  >
                    <div className="flex items-center gap-3.5">
                      <div className="w-11 h-11 rounded-xl bg-[#EBF3F8] text-[#2C5E7A] flex items-center justify-center flex-shrink-0">
                        <BookOpen className="w-5 h-5" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="font-semibold text-sm text-[#242321]">Enter a Topic</div>
                        <div className="text-xs text-[#77736D] leading-relaxed">
                          Type subject & topic for topic map generation.
                        </div>
                      </div>
                      <ChevronRight className="w-4 h-4 text-[#77736D] flex-shrink-0" />
                    </div>
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="font-semibold text-sm text-[#242321]">My Learning Library</span>
                    <span onClick={() => setActiveTab('learning')} className="text-xs font-medium text-[#C0584A] cursor-pointer">View All</span>
                  </div>
                  <div className="space-y-2">
                    {sampleTopics.map((item) => (
                      <div
                        key={item.id}
                        onClick={() => setActiveScreen('topic_review')}
                        className="p-3 bg-white rounded-xl border border-[#E2DDD3] flex items-center justify-between cursor-pointer shadow-xs"
                      >
                        <div>
                          <div className="text-[10px] text-[#77736D] font-medium">{item.subject} • {item.level}</div>
                          <div className="text-sm font-semibold text-[#242321]">{item.name}</div>
                        </div>
                        <span className="text-xs px-2 py-0.5 rounded font-semibold bg-[#F8EAE8] text-[#C0584A]">
                          {item.progress}%
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </>
            ) : activeTab === 'learning' ? (
              <div className="space-y-3">
                <h2 className="text-xl font-bold text-[#242321]">My Learning Library</h2>
                {sampleTopics.map((topic) => (
                  <div key={topic.id} className="p-4 bg-white rounded-xl border border-[#E2DDD3]">
                    <div className="text-xs font-semibold text-[#C0584A]">{topic.subject}</div>
                    <div className="font-bold text-base text-[#242321]">{topic.name}</div>
                    <div className="text-xs text-[#77736D] mt-1">{topic.completed} of {topic.total} concepts mastered</div>
                  </div>
                ))}
              </div>
            ) : activeTab === 'revision' ? (
              <div className="space-y-3">
                <h2 className="text-xl font-bold text-[#242321]">Revision & Spaced Retrieval</h2>
                <div className="p-4 bg-white rounded-xl border border-[#E2DDD3] space-y-2">
                  <div className="text-xs font-semibold text-[#C0584A]">Recall Item • Economics</div>
                  <div className="text-sm font-medium text-[#242321]">What is the primary economic reason governments sell state-owned enterprises?</div>
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                <h2 className="text-xl font-bold text-[#242321]">Settings & Profile</h2>
                <div className="p-4 bg-white rounded-xl border border-[#E2DDD3] space-y-2">
                  <div className="font-semibold text-sm text-[#242321]">Gemini API Key</div>
                  <input
                    type="password"
                    defaultValue="AIzaSyC-_SecureKeyConfigured_"
                    className="w-full p-2 bg-[#FAF8F3] border border-[#E2DDD3] rounded text-xs"
                    readOnly
                  />
                  <div className="text-[11px] text-[#77736D]">Stored securely in private device DataStore.</div>
                </div>
              </div>
            )}
          </div>

          {/* Bottom Navigation */}
          <div className="h-14 border-t border-[#E2DDD3] bg-[#F2EEE4] px-4 flex items-center justify-around z-20">
            <button
              onClick={() => { setActiveTab('home'); setActiveScreen('main'); }}
              className={`flex flex-col items-center gap-1 ${activeTab === 'home' && activeScreen === 'main' ? 'text-[#C0584A]' : 'text-[#77736D]'}`}
            >
              <FileText className="w-4 h-4" />
              <span className="text-[10px] font-medium">Home</span>
            </button>
            <button
              onClick={() => { setActiveTab('learning'); setActiveScreen('main'); }}
              className={`flex flex-col items-center gap-1 ${activeTab === 'learning' ? 'text-[#C0584A]' : 'text-[#77736D]'}`}
            >
              <BookOpen className="w-4 h-4" />
              <span className="text-[10px] font-medium">My Learning</span>
            </button>
            <button
              onClick={() => { setActiveTab('revision'); setActiveScreen('main'); }}
              className={`flex flex-col items-center gap-1 ${activeTab === 'revision' ? 'text-[#C0584A]' : 'text-[#77736D]'}`}
            >
              <RotateCw className="w-4 h-4" />
              <span className="text-[10px] font-medium">Revision</span>
            </button>
            <button
              onClick={() => { setActiveTab('profile'); setActiveScreen('main'); }}
              className={`flex flex-col items-center gap-1 ${activeTab === 'profile' ? 'text-[#C0584A]' : 'text-[#77736D]'}`}
            >
              <User className="w-4 h-4" />
              <span className="text-[10px] font-medium">Profile</span>
            </button>
          </div>

          <div className="h-4 bg-[#F2EEE4] flex items-center justify-center pb-1">
            <div className="w-32 h-1 bg-[#242321]/30 rounded-full" />
          </div>
        </div>
      </div>
    </div>
  );
}
