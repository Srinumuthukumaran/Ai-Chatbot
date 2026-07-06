let activeChatId = null;
let chatHistory = [];
const DEFAULT_USER_ID = 1; 
const API_BASE = window.location.port === '8080' ? '' : 'http://localhost:8080';

const sidebar = document.getElementById('sidebar');
const mobileMenuBtn = document.getElementById('mobileMenuBtn');
const mobileCloseBtn = document.getElementById('mobileCloseBtn');
const themeToggleBtn = document.getElementById('themeToggleBtn');
const newChatBtn = document.getElementById('newChatBtn');
const searchInput = document.getElementById('searchInput');
const chatList = document.getElementById('chatList');
const currentChatTitle = document.getElementById('currentChatTitle');
const chatBody = document.getElementById('chatBody');
const welcomeContainer = document.getElementById('welcomeContainer');
const chatForm = document.getElementById('chatForm');
const messageInput = document.getElementById('messageInput');
const sendBtn = document.getElementById('sendBtn');
const toastContainer = document.getElementById('toastContainer');

let confirmModal = null;
let confirmDeleteBtn = null;
let cancelDeleteBtn = null;
let closeModalBtn = null;
let pendingDeleteChatId = null;


function initializeApp() {
    confirmModal = document.getElementById('confirm-modal');
    confirmDeleteBtn = document.getElementById('confirm-delete-btn');
    cancelDeleteBtn = document.getElementById('cancel-delete-btn');
    closeModalBtn = confirmModal ? confirmModal.querySelector('.close-modal-btn') : null;

    initTheme();
    loadChatHistory();
    setupEventListeners();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeApp);
} else {
    initializeApp();
}
function setupEventListeners() {
    mobileMenuBtn.addEventListener('click', () => sidebar.classList.add('open'));
    mobileCloseBtn.addEventListener('click', () => sidebar.classList.remove('open'));

    themeToggleBtn.addEventListener('click', toggleTheme);

    newChatBtn.addEventListener('click', resetToNewChat);

    searchInput.addEventListener('input', filterChatHistory);

    chatForm.addEventListener('submit', handleFormSubmit);

    if (cancelDeleteBtn) cancelDeleteBtn.addEventListener('click', closeConfirmModal);
    if (closeModalBtn) closeModalBtn.addEventListener('click', closeConfirmModal);
    if (confirmModal) {
        confirmModal.addEventListener('click', (e) => {
            if (e.target === confirmModal) closeConfirmModal();
        });
    }

    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener('click', async () => {
            if (!pendingDeleteChatId) return;
            const chatId = pendingDeleteChatId;
            closeConfirmModal();

            try {
                const response = await fetch(`${API_BASE}/api/chat/${chatId}`, { method: 'DELETE' });
                if (!response.ok) {
                    let errMsg = 'Deletion failed';
                    try {
                        const errData = await response.json();
                        errMsg = errData.message || errMsg;
                    } catch (e) {}
                    throw new Error(errMsg);
                }
                
                showToast('Chat deleted successfully', 'success');

                if (activeChatId === chatId) {
                    resetToNewChat();
                }

                loadChatHistory();
            } catch (error) {
                console.error(error);
                showToast('Error deleting conversation', 'error');
            }
        });
    }

    sendBtn.addEventListener('click', createRippleEffect);
    messageInput.addEventListener('input', autoGrowTextArea);
    messageInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            chatForm.dispatchEvent(new Event('submit'));
        }
    });
}
function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'dark';
    if (savedTheme === 'light') {
        document.body.classList.add('light-theme');
        themeToggleBtn.innerHTML = '<i class="fa-solid fa-sun"></i>';
    } else {
        document.body.classList.remove('light-theme');
        themeToggleBtn.innerHTML = '<i class="fa-solid fa-moon"></i>';
    }
}

function toggleTheme() {
    const isLight = document.body.classList.toggle('light-theme');
    localStorage.setItem('theme', isLight ? 'light' : 'dark');
    themeToggleBtn.innerHTML = isLight ? '<i class="fa-solid fa-sun"></i>' : '<i class="fa-solid fa-moon"></i>';
    showToast('Theme updated', 'info');
}
async function loadChatHistory() {
    try {
        const response = await fetch(`${API_BASE}/api/chat/history?userId=${DEFAULT_USER_ID}`);
        if (!response.ok) {
            let errMsg = 'Failed to load chat history';
            try {
                const errData = await response.json();
                errMsg = errData.message || errMsg;
            } catch (e) {}
            throw new Error(errMsg);
        }
        chatHistory = await response.json();
        renderChatHistoryList();
    } catch (error) {
        console.error(error);
        showToast('Error loading chat history', 'error');
    }
}
async function loadChatMessages(chatId) {
    try {
        const response = await fetch(`${API_BASE}/api/chat/${chatId}`);
        if (!response.ok) {
            let errMsg = 'Failed to retrieve messages';
            try {
                const errData = await response.json();
                errMsg = errData.message || errMsg;
            } catch (e) {}
            throw new Error(errMsg);
        }
        const messages = await response.json();
        
        welcomeContainer.style.display = 'none';
        clearChatBubbles();

        messages.forEach(msg => {
            appendMessageBubble(msg.role, msg.content, msg.timestamp, false);
        });

        activeChatId = chatId;
        const currentChat = chatHistory.find(c => c.id === chatId);
        if (currentChat) {
            currentChatTitle.textContent = currentChat.title;
        }

        highlightActiveHistoryItem(chatId);
        scrollToBottom();
        
        sidebar.classList.remove('open');

    } catch (error) {
        console.error(error);
        showToast('Failed to load conversation messages', 'error');
    }
}

async function handleFormSubmit(e) {
    e.preventDefault();
    const prompt = messageInput.value.trim();
    if (!prompt) return;

    messageInput.value = '';
    messageInput.style.height = 'auto';
    welcomeContainer.style.display = 'none';

    const userTimestamp = new Date().toISOString();
    appendMessageBubble('user', prompt, userTimestamp, false);
    scrollToBottom();

    const typingIndicator = appendTypingIndicator();
    scrollToBottom();

    sendBtn.disabled = true;
    messageInput.disabled = true;

    try {
        const response = await fetch(`${API_BASE}/api/chat/send`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                content: prompt,
                chatId: activeChatId,
                userId: DEFAULT_USER_ID
            })
        });

        if (!response.ok) {
            let errMsg = 'API request failed';
            try {
                const errData = await response.json();
                errMsg = errData.message || errMsg;
            } catch (jsonErr) {
                try {
                    const text = await response.text();
                    errMsg = text.substring(0, 150) || errMsg;
                } catch (textErr) {
                    errMsg = `HTTP error ${response.status}: ${response.statusText}`;
                }
            }
            throw new Error(errMsg);
        }

        let data;
        try {
            data = await response.json();
        } catch (jsonErr) {
            throw new Error("Invalid response format received from server.");
        }

        typingIndicator.remove();

        appendMessageBubble('assistant', data.content, data.timestamp, true);
        
        const wasNewChat = (activeChatId === null);
        activeChatId = data.chatId;
        currentChatTitle.textContent = data.chatTitle;

        await loadChatHistory();
        
        if (wasNewChat) {
            highlightActiveHistoryItem(activeChatId);
        }

    } catch (error) {
        console.error(error);
        typingIndicator.remove();
        showToast(error.message || 'Error getting response', 'error');
        messageInput.value = prompt;
    } finally {
        sendBtn.disabled = false;
        messageInput.disabled = false;
        messageInput.focus();
    }
}

function deleteConversation(chatId) {
    pendingDeleteChatId = chatId;
    if (confirmModal) confirmModal.classList.add('show');
}

function closeConfirmModal() {
    if (confirmModal) confirmModal.classList.remove('show');
    pendingDeleteChatId = null;
}
function renderChatHistoryList() {
    chatList.innerHTML = '';
    
    if (chatHistory.length === 0) {
        chatList.innerHTML = '<div class="chat-list-empty">No conversations found</div>';
        return;
    }

    chatHistory.forEach(chat => {
        const item = document.createElement('div');
        item.className = `history-item ${activeChatId === chat.id ? 'active' : ''}`;
        item.dataset.id = chat.id;
        item.addEventListener('click', () => loadChatMessages(chat.id));

        item.innerHTML = `
            <div class="item-left">
                <i class="fa-regular fa-message"></i>
                <span class="item-title" title="${escapeHtml(chat.title)}">${escapeHtml(chat.title)}</span>
            </div>
            <button class="item-delete-btn" title="Delete conversation">
                <i class="fa-regular fa-trash-can"></i>
            </button>
        `;

        const delBtn = item.querySelector('.item-delete-btn');
        delBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            e.preventDefault();
            deleteConversation(chat.id);
        });

        chatList.appendChild(item);
    });
}

function resetToNewChat() {
    activeChatId = null;
    currentChatTitle.textContent = 'Default Assistant';
    clearChatBubbles();
    welcomeContainer.style.display = 'flex';
    highlightActiveHistoryItem(null);
    messageInput.value = '';
    messageInput.style.height = 'auto';
    sidebar.classList.remove('open');
}

function clearChatBubbles() {
    const bubbles = chatBody.querySelectorAll('.message-row');
    bubbles.forEach(b => b.remove());
}

function prefillInput(text) {
    messageInput.value = text;
    autoGrowTextArea();
    messageInput.focus();
}

function highlightActiveHistoryItem(chatId) {
    const items = chatList.querySelectorAll('.history-item');
    items.forEach(item => {
        if (parseInt(item.dataset.id) === chatId) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });
}

function filterChatHistory() {
    const query = searchInput.value.toLowerCase();
    const items = chatList.querySelectorAll('.history-item');
    items.forEach(item => {
        const title = item.querySelector('.item-title').textContent.toLowerCase();
        if (title.includes(query)) {
            item.style.display = 'flex';
        } else {
            item.style.display = 'none';
        }
    });
}

function scrollToBottom() {
    chatBody.scrollTo({
        top: chatBody.scrollHeight,
        behavior: 'smooth'
    });
}

function autoGrowTextArea() {
    messageInput.style.height = 'auto';
    messageInput.style.height = (messageInput.scrollHeight) + 'px';
}

function appendMessageBubble(role, content, timestamp, useTypewriter = false) {
    const row = document.createElement('div');
    row.className = `message-row ${role}-row`;

    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';
    
    const meta = document.createElement('div');
    meta.className = 'message-meta';
    
    const formattedTime = formatTimestamp(timestamp);
    meta.innerHTML = `<span class="message-time">${formattedTime}</span>`;

    row.appendChild(bubble);

    if (useTypewriter && role === 'assistant') {
        chatBody.appendChild(row);
        typewriterEffect(bubble, content, () => {
            bubble.appendChild(meta);
            scrollToBottom();
        });
    } else {
        bubble.innerHTML = parseMarkdown(content);
        bubble.appendChild(meta);
        chatBody.appendChild(row);
    }
}

function appendTypingIndicator() {
    const row = document.createElement('div');
    row.className = 'message-row assistant-row';

    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';
    
    bubble.innerHTML = `
        <div class="typing-indicator">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
        </div>
    `;

    row.appendChild(bubble);
    chatBody.appendChild(row);
    return row;
}

function typewriterEffect(element, text, callback) {
    let index = 0;
    const speed = 12; 
    
    const htmlContent = parseMarkdown(text);
    
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = htmlContent;
    
    const nodes = Array.from(tempDiv.childNodes);
    let currentNodeIndex = 0;
    
    function printNextNode() {
        if (currentNodeIndex >= nodes.length) {
            if (callback) callback();
            return;
        }

        const node = nodes[currentNodeIndex];
        
        if (node.nodeType === Node.TEXT_NODE) {
            const textVal = node.textContent;
            let charIndex = 0;
            const textNode = document.createTextNode('');
            element.appendChild(textNode);
            
            function printChar() {
                if (charIndex < textVal.length) {
                    textNode.textContent += textVal[charIndex];
                    charIndex++;
                    scrollToBottom();
                    setTimeout(printChar, speed);
                } else {
                    currentNodeIndex++;
                    printNextNode();
                }
            }
            printChar();
        } else {
            const clone = node.cloneNode(true);
            element.appendChild(clone);
            currentNodeIndex++;
            scrollToBottom();
            setTimeout(printNextNode, speed * 2);
        }
    }
    
    printNextNode();
}
function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = '<i class="fa-solid fa-circle-info"></i>';
    if (type === 'success') icon = '<i class="fa-solid fa-circle-check"></i>';
    if (type === 'error') icon = '<i class="fa-solid fa-triangle-exclamation"></i>';

    toast.innerHTML = `
        ${icon}
        <span>${message}</span>
    `;

    toastContainer.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'messageFadeIn 0.3s reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function parseMarkdown(text) {
    if (!text) return "";
    let html = text;
    
    html = escapeHtml(html);
    
    html = html.replace(/```(\w*)\n([\s\S]*?)\n```/g, (match, lang, code) => {
        return `<pre><code class="language-${lang}">${code.trim()}</code></pre>`;
    });
    
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    
    html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    
    html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
    
    html = html.replace(/\n/g, '<br>');
    
    return html;
}

function escapeHtml(str) {
    return str.replace(/&/g, "&amp;")
              .replace(/</g, "&lt;")
              .replace(/>/g, "&gt;")
              .replace(/"/g, "&quot;")
              .replace(/'/g, "&#039;");
}

function formatTimestamp(isoStr) {
    try {
        const date = new Date(isoStr);
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
        return '';
    }
}

function createRippleEffect(e) {
    const button = e.currentTarget;
    const ripple = document.createElement('span');
    ripple.className = 'ripple';
    
    const rect = button.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height);
    
    ripple.style.width = ripple.style.height = `${size}px`;
    ripple.style.left = `${e.clientX - rect.left - size/2}px`;
    ripple.style.top = `${e.clientY - rect.top - size/2}px`;
    
    button.appendChild(ripple);
    
    setTimeout(() => {
        ripple.remove();
    }, 600);
}
