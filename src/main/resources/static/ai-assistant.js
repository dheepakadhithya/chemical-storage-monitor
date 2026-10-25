/**
 * SafeChem AI Assistant - Voice & Chat Interface
 * Connects directly to Spring Boot /api/ai/chat
 */
(function() {
    'use strict';

    // State
    let isTtsEnabled = false;
    let isListening = false;
    let isSubmitting = false;
    let recognition = null;
    let voiceDebounceTimer = null;
    let pendingVoiceText = '';

    // 1. Inject DOM Elements for AI FAB and Chat Window
    function injectAiWidget() {
        if (document.getElementById('ai-fab-btn')) return;

        // Create FAB
        const fab = document.createElement('button');
        fab.id = 'ai-fab-btn';
        fab.title = 'AI Assistant (Voice & Chat)';
        fab.innerHTML = `
            <i class="fa-solid fa-wand-magic-sparkles"></i>
            <span class="fab-badge"></span>
        `;
        document.body.appendChild(fab);

        // Create Chat Window
        const chatWin = document.createElement('div');
        chatWin.id = 'ai-chat-window';
        chatWin.innerHTML = `
            <div class="ai-header">
                <div class="ai-header-left">
                    <div class="ai-header-avatar">
                        <i class="fa-solid fa-robot"></i>
                    </div>
                    <div>
                        <div class="ai-header-title">SafeChem Assistant</div>
                        <div class="ai-header-status">
                            <span class="ai-status-dot"></span>
                            <span id="ai-status-text">Ollama Active (Llama 3.2)</span>
                        </div>
                    </div>
                </div>
                <div class="ai-header-actions">
                    <button id="ai-tts-toggle" class="ai-header-btn" title="Toggle Voice Speech Output">
                        <i class="fa-solid fa-volume-xmark"></i>
                    </button>
                    <button id="ai-close-btn" class="ai-header-btn" title="Close Chat">
                        <i class="fa-solid fa-xmark"></i>
                    </button>
                </div>
            </div>

            <div class="ai-quick-chips">
                <button class="ai-chip" data-cmd="What is the current cabinet status?">🌡️ Status</button>
                <button class="ai-chip" data-cmd="Please turn on the cooling fan.">❄️ Cool Fan</button>
                <button class="ai-chip" data-cmd="Lock the cabinet.">🔒 Lock</button>
                <button class="ai-chip" data-cmd="Unlock the cabinet.">🔓 Unlock</button>
                <button class="ai-chip" data-cmd="What chemicals are stored in the cabinet?">🧪 Inventory</button>
                <button class="ai-chip" data-cmd="Silence the buzzer alarm.">🔕 Silence Alarm</button>
            </div>

            <div id="ai-messages" class="ai-messages-body">
                <div class="ai-msg assistant">
                    <div class="ai-msg-bubble">
                        Hello! I am your <strong>SafeChem AI Assistant</strong>. 
                        You can type or click the microphone to speak. I can adjust cooling fans, control locks, silence alarms, and monitor chemical safety.
                    </div>
                </div>
            </div>

            <div class="ai-input-bar">
                <button id="ai-mic-btn" class="ai-mic-btn" title="Click to speak (one command at a time)">
                    <i class="fa-solid fa-microphone"></i>
                </button>
                <input id="ai-text-input" type="text" placeholder="Speak or type a command..." autocomplete="off">
                <button id="ai-send-btn" class="ai-send-btn" title="Send message">
                    <i class="fa-solid fa-paper-plane"></i>
                </button>
            </div>
        `;
        document.body.appendChild(chatWin);

        initWidgetEvents();
        initSpeechRecognition();
    }

    // 2. Wire Up Events
    function initWidgetEvents() {
        const fab = document.getElementById('ai-fab-btn');
        const win = document.getElementById('ai-chat-window');
        const closeBtn = document.getElementById('ai-close-btn');
        const ttsBtn = document.getElementById('ai-tts-toggle');
        const sendBtn = document.getElementById('ai-send-btn');
        const input = document.getElementById('ai-text-input');
        const micBtn = document.getElementById('ai-mic-btn');
        const chips = document.querySelectorAll('.ai-chip');

        // Toggle Open/Close
        fab.addEventListener('click', () => {
            win.classList.toggle('active');
            if (win.classList.contains('active')) {
                input.focus();
            }
        });

        closeBtn.addEventListener('click', () => {
            win.classList.remove('active');
        });

        // TTS Toggle
        ttsBtn.addEventListener('click', () => {
            isTtsEnabled = !isTtsEnabled;
            ttsBtn.innerHTML = isTtsEnabled 
                ? '<i class="fa-solid fa-volume-high" style="color: #38bdf8;"></i>' 
                : '<i class="fa-solid fa-volume-xmark"></i>';
            ttsBtn.title = isTtsEnabled ? 'Voice Output: ON' : 'Voice Output: OFF';
        });

        // Send on Button Click
        sendBtn.addEventListener('click', () => {
            if (!isSubmitting) submitUserMessage();
        });

        // Send on Enter Key
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                if (!isSubmitting) submitUserMessage();
            }
        });

        // Quick action chips
        chips.forEach(chip => {
            chip.addEventListener('click', () => {
                if (isSubmitting) return;
                const cmd = chip.getAttribute('data-cmd');
                if (cmd) {
                    input.value = cmd;
                    submitUserMessage();
                }
            });
        });

        // Mic Button Click
        micBtn.addEventListener('click', () => toggleSpeechRecognition());
    }

    // 3. Speech Recognition Setup (Web Speech API)
    function initSpeechRecognition() {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SpeechRecognition) {
            console.warn('Web Speech API not supported in this browser.');
            const micBtn = document.getElementById('ai-mic-btn');
            if (micBtn) {
                micBtn.title = 'Voice recognition not supported in this browser. Use Chrome or Edge.';
                micBtn.style.opacity = '0.5';
            }
            return;
        }

        recognition = new SpeechRecognition();
        recognition.continuous = false;
        recognition.interimResults = true; // Show words live as you speak
        recognition.lang = 'en-US';

        recognition.onstart = () => {
            isListening = true;
            pendingVoiceText = '';
            if (voiceDebounceTimer) clearTimeout(voiceDebounceTimer);

            const micBtn = document.getElementById('ai-mic-btn');
            if (micBtn) micBtn.classList.add('listening');
            const input = document.getElementById('ai-text-input');
            if (input) input.placeholder = 'Listening... Speak your command';
        };

        recognition.onresult = (event) => {
            let combined = '';
            for (let i = event.resultIndex; i < event.results.length; ++i) {
                combined += event.results[i][0].transcript;
            }
            combined = combined.trim();
            if (!combined) return;

            pendingVoiceText = combined;
            const input = document.getElementById('ai-text-input');
            if (input) input.value = pendingVoiceText;

            // Debounce: Wait 750ms after user pauses speaking before submitting ONCE
            if (voiceDebounceTimer) clearTimeout(voiceDebounceTimer);
            voiceDebounceTimer = setTimeout(() => {
                if (pendingVoiceText.length >= 3 && !isSubmitting) {
                    if (recognition) recognition.stop();
                    submitUserMessage();
                }
            }, 750);
        };

        recognition.onerror = (event) => {
            console.warn('Speech recognition warning:', event.error);
            stopListening();
        };

        recognition.onend = () => {
            stopListening();
            // If the user spoke and stopped, but debounce hasn't triggered yet:
            if (pendingVoiceText && pendingVoiceText.length >= 3 && !isSubmitting) {
                submitUserMessage();
            }
            pendingVoiceText = '';
        };
    }

    function toggleSpeechRecognition() {
        if (!recognition) {
            alert('Voice recognition is supported in Google Chrome and Microsoft Edge. Please use a supported browser or type your request.');
            return;
        }

        if (isListening) {
            recognition.stop();
        } else {
            try {
                recognition.start();
            } catch (err) {
                console.warn('Speech recognition start failed:', err);
            }
        }
    }

    function stopListening() {
        isListening = false;
        if (voiceDebounceTimer) clearTimeout(voiceDebounceTimer);
        const micBtn = document.getElementById('ai-mic-btn');
        if (micBtn) micBtn.classList.remove('listening');
        const input = document.getElementById('ai-text-input');
        if (input) input.placeholder = 'Speak or type a command...';
    }

    // 4. Submit message to /api/ai/chat (Single-flight execution)
    async function submitUserMessage() {
        if (isSubmitting) return; // Prevent multiple simultaneous prompts!

        const input = document.getElementById('ai-text-input');
        const text = input ? input.value.trim() : '';

        // Ignore single syllables or noise
        if (!text || text.length < 2) return;

        isSubmitting = true;
        if (input) input.value = '';
        stopListening();

        appendMessage('user', text);
        showTypingIndicator();

        try {
            const response = await fetch('/api/ai/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: text })
            });

            removeTypingIndicator();

            if (response.ok) {
                const data = await response.json();
                appendMessage('assistant', data.response, data.actions);

                // Voice output if enabled
                if (isTtsEnabled && data.response) {
                    speakText(data.response);
                }

                // If hardware actions were executed, reload dashboard states immediately
                if (data.actions && data.actions.length > 0) {
                    if (typeof window.fetchDeviceControls === 'function') {
                        window.fetchDeviceControls();
                    }
                    if (typeof window.fetchLatestReading === 'function') {
                        window.fetchLatestReading();
                    }
                }
            } else {
                appendMessage('assistant', 'Sorry, I encountered an issue connecting to the AI service. (HTTP ' + response.status + ')');
            }
        } catch (err) {
            removeTypingIndicator();
            console.error('Error contacting AI chat endpoint:', err);
            appendMessage('assistant', 'Network error reaching the AI service. Please ensure the backend is running.');
        } finally {
            isSubmitting = false;
        }
    }

    // 5. Append message bubble to UI
    function appendMessage(role, text, actions) {
        const container = document.getElementById('ai-messages');
        if (!container) return;

        const msgDiv = document.createElement('div');
        msgDiv.className = `ai-msg ${role}`;

        let formattedText = escapeHtml(text).replace(/\n/g, '<br>');
        // Format bold markdown
        formattedText = formattedText.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

        let actionsHtml = '';
        if (actions && actions.length > 0) {
            actionsHtml = actions.map(act => `
                <div class="ai-action-badge">
                    <i class="fa-solid fa-bolt"></i> ${escapeHtml(act)}
                </div>
            `).join('');
        }

        msgDiv.innerHTML = `
            <div class="ai-msg-bubble">
                ${formattedText}
                ${actionsHtml}
            </div>
        `;

        container.appendChild(msgDiv);
        container.scrollTop = container.scrollHeight;
    }

    // 6. Typing Indicator
    function showTypingIndicator() {
        const container = document.getElementById('ai-messages');
        if (!container || document.getElementById('ai-typing-indicator')) return;

        const typingDiv = document.createElement('div');
        typingDiv.id = 'ai-typing-indicator';
        typingDiv.className = 'ai-msg assistant';
        typingDiv.innerHTML = `
            <div class="ai-typing">
                <span class="ai-typing-dot"></span>
                <span class="ai-typing-dot"></span>
                <span class="ai-typing-dot"></span>
            </div>
        `;
        container.appendChild(typingDiv);
        container.scrollTop = container.scrollHeight;
    }

    function removeTypingIndicator() {
        const el = document.getElementById('ai-typing-indicator');
        if (el) el.remove();
    }

    // 7. Text To Speech (TTS)
    function speakText(text) {
        if (!window.speechSynthesis) return;
        window.speechSynthesis.cancel(); // stop any current speech
        const cleanSpeech = text.replace(/[*#_]/g, '');
        const utterance = new SpeechSynthesisUtterance(cleanSpeech);
        utterance.rate = 1.0;
        utterance.pitch = 1.0;
        window.speechSynthesis.speak(utterance);
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    // Auto-initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', injectAiWidget);
    } else {
        injectAiWidget();
    }
})();
