document.addEventListener('DOMContentLoaded', () => {
    initPasswordToggles();
    initStatusTabs();
    initTableSearch();
    initPracticeAreaLimit();
    initLegalGuide();
});

/* Show / hide password on every password field */
function initPasswordToggles() {
    document.querySelectorAll('input[type=password]').forEach(inp => {
        const lab = document.createElement('label');
        lab.className = 'showpw';
        const cb = document.createElement('input');
        cb.type = 'checkbox';
        cb.addEventListener('change', () => {
            inp.type = cb.checked ? 'text' : 'password';
        });
        lab.append(cb, document.createTextNode(' Show password'));
        inp.insertAdjacentElement('afterend', lab);
    });
}

/* Status filter tabs for appointment tables */
function initStatusTabs() {
    document.querySelectorAll('[data-tabs]').forEach(bar => {
        const table = document.getElementById(bar.dataset.tabs);
        if (!table) {
            return;
        }
        bar.querySelectorAll('.tab').forEach(tab => tab.addEventListener('click', () => {
            bar.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            const f = tab.dataset.filter;
            table.querySelectorAll('tr[data-group]').forEach(row => {
                row.hidden = (f !== 'ALL' && row.dataset.group !== f) || row.dataset.searchMatch === 'no';
            });
        }));
    });
}

/* Live search inside client and advocate appointment tables */
function initTableSearch() {
    document.querySelectorAll('[data-table-search]').forEach(input => {
        const table = document.getElementById(input.dataset.tableSearch);
        if (!table) {
            return;
        }
        input.addEventListener('input', () => {
            const term = input.value.trim().toLowerCase();
            table.querySelectorAll('tr[data-group]').forEach(row => {
                row.dataset.searchMatch = row.innerText.toLowerCase().includes(term) ? 'yes' : 'no';
                row.hidden = row.dataset.searchMatch === 'no';
            });
            const activeTab = document.querySelector('[data-tabs="' + input.dataset.tableSearch + '"] .tab.active');
            if (activeTab) {
                const filter = activeTab.dataset.filter;
                table.querySelectorAll('tr[data-group]').forEach(row => {
                    row.hidden = row.dataset.searchMatch === 'no' || (filter !== 'ALL' && row.dataset.group !== filter);
                });
            }
        });
    });
}

/* Profile page: allow at most 4 practice areas */
function initPracticeAreaLimit() {
    const checks = document.getElementById('specChecks');
    if (!checks) {
        return;
    }
    const boxes = checks.querySelectorAll('input[type=checkbox]');
    const limit = () => {
        const n = [...boxes].filter(b => b.checked).length;
        boxes.forEach(b => {
            b.disabled = !b.checked && n >= 4;
        });
    };
    boxes.forEach(b => b.addEventListener('change', limit));
    limit();
}

/*
 * AI legal guide chat.
 * The conversation itself is kept on the server (in the session), so every question is sent on its own
 * and Gemini still answers follow-ups in context. "New chat" asks the server to forget it.
 */
function initLegalGuide() {
    const form = document.getElementById('aiForm');
    if (!form) {
        return;
    }
    const role = form.dataset.role || '';
    const box = document.getElementById('chatlog');
    const state = document.getElementById('aiState');
    const input = document.getElementById('question');
    const sendBtn = document.getElementById('aiSend');
    const resetBtn = document.getElementById('aiReset');
    const greeting = box.firstElementChild ? box.firstElementChild.textContent : '';
    const idleHint = state.textContent;
    const firstPlaceholder = input.placeholder;
    let busy = false;

    const scrollDown = () => {
        box.scrollTop = box.scrollHeight;
    };

    const bubble = (cls, text) => {
        const d = document.createElement('div');
        d.className = 'bubble ' + cls;
        d.textContent = text;
        box.appendChild(d);
        scrollDown();
        return d;
    };

    /* quick-start chips above the chat: fill the box with a ready-made question */
    document.querySelectorAll('[data-fill]').forEach(btn => btn.addEventListener('click', () => {
        input.value = btn.dataset.fill;
        input.focus();
    }));

    /* add a suggested keyword to whatever the person is typing */
    const addKeyword = (chip, word) => {
        const current = input.value.replace(/\s+$/, '');
        const alreadyThere = current.toLowerCase().includes(word.toLowerCase());
        if (!alreadyThere) {
            input.value = current === '' ? word : current + (/[.,;!?]$/.test(current) ? ' ' : ', ') + word;
        }
        chip.disabled = true;
        input.focus();
    };

    const removeOldSuggestions = () => {
        box.querySelectorAll('.kw-suggest').forEach(n => n.remove());
    };

    const showKeywordSuggestions = (list, title) => {
        if (!list || !list.length) {
            return;
        }
        const wrap = document.createElement('div');
        wrap.className = 'bubble bot kw-suggest';
        const h = document.createElement('b');
        h.textContent = title || 'Add more keywords:';
        const chips = document.createElement('div');
        chips.className = 'chips';
        list.forEach(word => {
            const chip = document.createElement('button');
            chip.type = 'button';
            chip.className = 'chip';
            chip.textContent = '+ ' + word;
            chip.addEventListener('click', () => addKeyword(chip, word));
            chips.appendChild(chip);
        });
        wrap.append(h, chips);
        box.appendChild(wrap);
        scrollDown();
    };

    const showAdvocates = (list, topic) => {
        if (!list || !list.length) {
            return;
        }
        const wrap = document.createElement('div');
        wrap.className = 'bubble bot suggest';
        const h = document.createElement('b');
        h.textContent = 'Suggested advocates';
        wrap.appendChild(h);
        list.forEach(a => {
            const card = document.createElement('div');
            card.className = 'adv-card';
            if (a.photo) {
                const photo = document.createElement('img');
                photo.className = 'advocate-photo mini-photo';
                photo.src = a.photo;
                photo.alt = '';
                card.appendChild(photo);
            }
            const name = document.createElement('strong');
            name.textContent = a.name;
            const tags = document.createElement('div');
            tags.className = 'tags';
            String(a.specialisation || '').split(',').forEach(t => {
                if (!t.trim()) {
                    return;
                }
                const s = document.createElement('span');
                s.className = 'tag';
                s.textContent = t.trim();
                tags.appendChild(s);
            });
            const meta = document.createElement('small');
            meta.className = 'muted';
            const parts = [];
            parts.push(a.reviews > 0 ? '★ ' + Number(a.rating).toFixed(1) + ' (' + a.reviews + ')' : 'New');
            parts.push(a.experience + ' years experience');
            if (a.city) {
                parts.push(a.city);
            }
            parts.push(a.fee > 0 ? '₹' + a.fee + ' per consultation' : 'Fee on request');
            meta.textContent = parts.join(' · ') + (a.bio ? '\n' + a.bio : '');
            card.append(name, tags, meta);
            if (role === 'CLIENT') {
                const link = document.createElement('a');
                link.className = 'btn primary';
                link.href = 'client.jsp?advocate=' + encodeURIComponent(a.id) + '&topic=' + encodeURIComponent(topic || '') + '#book';
                link.textContent = 'Book consultation';
                card.appendChild(link);
            } else if (role === '') {
                const link = document.createElement('a');
                link.className = 'btn primary';
                link.href = 'login.jsp';
                link.textContent = 'Sign in to book';
                card.appendChild(link);
            }
            wrap.appendChild(card);
        });
        box.appendChild(wrap);
        scrollDown();
    };

    const statusText = data => {
        if (data.gemini === 'ok') {
            const how = data.mode === 'gemini'
                ? 'Answered by Gemini without needing keywords.'
                : 'Keyword match and Gemini explanation combined.';
            return how + ' You can ask a follow-up question. Review all information with a qualified advocate.';
        }
        return data.geminiMessage || 'Review all information with a qualified advocate.';
    };

    const setBusy = value => {
        busy = value;
        sendBtn.disabled = value;
        input.disabled = value;
        if (!value) {
            input.focus();
        }
    };

    form.addEventListener('submit', async e => {
        e.preventDefault();
        const value = input.value.trim();
        if (!value || busy) {
            return;
        }
        removeOldSuggestions();
        bubble('user', value);
        input.value = '';
        const typing = bubble('bot typing', 'Preparing a response');
        state.textContent = 'Preparing a response…';
        setBusy(true);
        try {
            const res = await fetch('ai-assist', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                body: new URLSearchParams({ question: value })
            });
            const data = await res.json();
            typing.remove();
            bubble('bot', data.reply || 'No response available.');
            showAdvocates(data.advocates, (data.categories || [])[0]);
            showKeywordSuggestions(data.suggestions, data.suggestTitle);
            state.textContent = statusText(data);
            input.placeholder = 'Ask a follow-up question or add more details…';
            resetBtn.hidden = false;
        } catch (err) {
            typing.remove();
            bubble('bot', 'Sorry, something went wrong. Please try again.');
            state.textContent = 'Could not connect. Please try again.';
            input.value = value;
        }
        setBusy(false);
        scrollDown();
    });

    /* Enter sends, Shift+Enter adds a new line */
    input.addEventListener('keydown', e => {
        if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
            e.preventDefault();
            if (form.requestSubmit) {
                form.requestSubmit();
            } else {
                sendBtn.click();
            }
        }
    });

    /* start a fresh conversation */
    resetBtn.addEventListener('click', async () => {
        if (busy) {
            return;
        }
        try {
            await fetch('ai-assist', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                body: new URLSearchParams({ reset: '1' })
            });
        } catch (err) {
            /* the page still starts clean; the server forgets on the next page load */
        }
        box.textContent = '';
        bubble('bot', greeting);
        input.value = '';
        input.placeholder = firstPlaceholder;
        state.textContent = idleHint;
        resetBtn.hidden = true;
        input.focus();
    });
}
