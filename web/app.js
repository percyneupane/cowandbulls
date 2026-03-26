const guessesMadeEl = document.getElementById("guesses-made");
const guessesLeftEl = document.getElementById("guesses-left");
const messageEl = document.getElementById("message");
const historyListEl = document.getElementById("history-list");
const guessForm = document.getElementById("guess-form");
const guessInput = document.getElementById("guess-input");
const newGameButton = document.getElementById("new-game-button");

async function fetchJson(url, options = {}) {
    const response = await fetch(url, {
        headers: {
            "Content-Type": "application/json"
        },
        ...options
    });

    const data = await response.json();
    if (!response.ok) {
        throw new Error(data.error || "Request failed.");
    }

    return data;
}

function renderHistory(history) {
    historyListEl.innerHTML = "";

    if (history.length === 0) {
        const empty = document.createElement("p");
        empty.className = "empty-state";
        empty.textContent = "No guesses yet. Start with a five-digit code.";
        historyListEl.appendChild(empty);
        return;
    }

    [...history].reverse().forEach((entry) => {
        const row = document.createElement("div");
        row.className = "history-row";

        const guess = document.createElement("span");
        guess.className = "history-guess";
        guess.textContent = entry.guess;

        const result = document.createElement("span");
        result.className = "history-result";
        result.textContent = entry.result.trim() || "-----";

        row.appendChild(guess);
        row.appendChild(result);
        historyListEl.appendChild(row);
    });
}

function renderState(state) {
    guessesMadeEl.textContent = state.guessesMade;
    guessesLeftEl.textContent = state.guessesRemaining;
    renderHistory(state.history);

    if (state.won) {
        messageEl.textContent = `You solved it in ${state.guessesMade} guesses.`;
        guessInput.disabled = true;
        return;
    }

    if (state.finished) {
        messageEl.textContent = `Game over. The secret code was ${state.revealCode}.`;
        guessInput.disabled = true;
        return;
    }

    if (state.guessesMade === 0) {
        messageEl.textContent = "Enter a five-digit guess to begin.";
    } else {
        messageEl.textContent = "Keep going.";
    }

    guessInput.disabled = false;
}

async function loadState() {
    const state = await fetchJson("/api/state");
    renderState(state);
}

guessForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const guess = guessInput.value.trim();

    try {
        const state = await fetchJson("/api/guess", {
            method: "POST",
            body: JSON.stringify({ guess })
        });
        guessInput.value = "";
        renderState(state);
    } catch (error) {
        messageEl.textContent = error.message;
    }
});

newGameButton.addEventListener("click", async () => {
    const state = await fetchJson("/api/start", { method: "POST" });
    guessInput.value = "";
    renderState(state);
});

loadState().catch((error) => {
    messageEl.textContent = error.message;
});
