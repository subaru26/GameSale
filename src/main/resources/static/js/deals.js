let dealsData = [];
let currentPage = 1;
const itemsPerPage = 48;
let totalFetched = 0;
let hasMoreData = true;
let selectedStores = [];
let isFetching = false;
let excludeDLC = false;
let currentSort = "default";
let currentDeal = null;
let isSearchMode = false;
let savedSearchQuery = '';
let searchHistory = []; // 検索履歴用の配列を追加

const container = document.getElementById("dealsContainer");
const prevBtn = document.getElementById("prevPage");
const nextBtn = document.getElementById("nextPage");
const scrollTopBtn = document.getElementById("scrollTopBtn");
const fetchBtn = document.getElementById("fetchDealsBtn");
const sortSelect = document.getElementById("sortSelect");
const searchInput = document.getElementById("searchInput");
const searchBtn = document.getElementById("searchBtn");
const clearSearchBtn = document.getElementById("clearSearchBtn");
const loadingMessage = document.getElementById("loadingMessage");
const filterSortArea = document.getElementById("filterSortArea");

const excludeDLCCheckbox = document.getElementById("excludeDLC");
if (excludeDLCCheckbox) {
  excludeDLC = excludeDLCCheckbox.checked;
  excludeDLCCheckbox.addEventListener("change", () => {
    excludeDLC = excludeDLCCheckbox.checked;
    currentPage = 1;
    renderPage();
  });
}

// --- 検索履歴関連の関数 ---
async function loadSearchHistory() {
  try {
    const res = await fetch('/api/search-history');
    const data = await res.json();
    if (data.success && data.history) {
      searchHistory = data.history;
    } else {
      searchHistory = [];
    }
  } catch (err) {
    console.error("[Search] Error loading history from DB:", err);
    searchHistory = [];
  }
}

async function addToSearchHistory(query) {
  if (!query || query.trim() === '') return;
  const trimmedQuery = query.trim();
  try {
    const res = await fetch('/api/search-history', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: trimmedQuery })
    });
    const data = await res.json();
    if (data.success) await loadSearchHistory();
  } catch (err) {
    console.error("[Search] Error adding history to DB:", err);
  }
}

function showSearchHistory() {
  const dropdown = document.getElementById('searchHistoryDropdown');
  if (!dropdown) return;
  if (searchHistory.length === 0) {
    dropdown.classList.add('hidden');
    return;
  }
  const inputValue = searchInput.value.toLowerCase();
  const filteredHistory = searchHistory.filter(h => h.toLowerCase().includes(inputValue));
  
  if (filteredHistory.length === 0 && inputValue === '') {
    filteredHistory.push(...searchHistory);
  }

  if (filteredHistory.length === 0) {
    dropdown.classList.add('hidden');
    return;
  }

  const bgClass = darkMode ? 'bg-gray-800 text-gray-100' : 'bg-white text-gray-800';
  const hoverClass = darkMode ? 'hover:bg-gray-700' : 'hover:bg-gray-100';
  const borderClass = darkMode ? 'border-gray-600' : 'border-gray-300';

  dropdown.innerHTML = filteredHistory.map(historyItem => {
    const escapedItem = historyItem.replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '&quot;');
    return `
    <div class="search-history-item flex items-center justify-between px-4 py-2 ${hoverClass} cursor-pointer border-b ${borderClass} last:border-b-0"
         data-query="${escapedItem}">
      <span class="flex-1">🔍 ${historyItem}</span>
      <button class="delete-history-btn text-red-500 hover:text-red-700 px-2" 
              data-query="${escapedItem}"
              onclick="event.stopPropagation(); deleteSearchHistory(event)">×</button>
    </div>`;
  }).join('');
  dropdown.classList.remove('hidden');

  dropdown.querySelectorAll('.search-history-item').forEach(item => {
    item.addEventListener('click', (e) => {
      if (e.target.classList.contains('delete-history-btn')) return;
      searchInput.value = item.dataset.query;
      hideSearchHistory();
    });
  });
}

function hideSearchHistory() {
  const dropdown = document.getElementById('searchHistoryDropdown');
  if (dropdown) dropdown.classList.add('hidden');
}

async function deleteSearchHistory(event) {
  event.stopPropagation();
  const query = event.target.closest('.search-history-item').dataset.query;
  try {
    const res = await fetch(`/api/search-history?query=${encodeURIComponent(query)}`, { method: 'DELETE' });
    const data = await res.json();
    if (data.success) {
      await loadSearchHistory();
      showSearchHistory();
    }
  } catch (err) {
    console.error("[Search] Error deleting history from DB:", err);
  }
}

// --- イベントリスナー (検索) ---
searchBtn.addEventListener("click", async () => {
  const query = searchInput.value.trim();
  if (!query) { alert("検索キーワードを入力してください。"); return; }
  if (isFetching) return;

  isFetching = true;
  searchBtn.disabled = true;
  searchBtn.textContent = "検索中...";
  loadingMessage.classList.remove("hidden");

  dealsData = [];
  currentPage = 1;
  isSearchMode = true;
  savedSearchQuery = query;

  try {
    const url = `/api/search?q=${encodeURIComponent(query)}`;
    console.log("[Search] Fetching URL:", url);

    const res = await fetch(url);
    const searchResults = await res.json();

    if (searchResults && searchResults.length > 0) {
      dealsData = searchResults;
      hasMoreData = false;
      clearSearchBtn.classList.remove("hidden");
      if (filterSortArea) filterSortArea.classList.add("hidden");
      
      addToSearchHistory(query).catch(console.error);
      localStorage.setItem('searchResults', JSON.stringify(searchResults));
      localStorage.setItem('searchQuery', query);
      localStorage.setItem('isSearchMode', 'true');
    } else {
      dealsData = [];
      alert("検索結果が見つかりませんでした。");
      localStorage.removeItem('searchResults');
      localStorage.removeItem('searchQuery');
      localStorage.removeItem('isSearchMode');
    }
  } catch (err) {
    console.error("[Search] Error:", err);
    alert("検索中にエラーが発生しました。");
  } finally {
    isFetching = false;
    searchBtn.disabled = false;
    searchBtn.textContent = "🔍 検索";
    loadingMessage.classList.add("hidden");
    renderPage();
  }
});

searchInput.addEventListener("keypress", (e) => {
  if (e.key === "Enter") searchBtn.click();
  else if (e.key === "Escape") hideSearchHistory();
});

searchInput.addEventListener('input', showSearchHistory);
searchInput.addEventListener('focus', () => { if (searchHistory.length > 0) showSearchHistory(); });
document.addEventListener('click', (e) => {
  const dropdown = document.getElementById('searchHistoryDropdown');
  const searchContainer = searchInput.closest('.relative');
  if (dropdown && searchContainer && !searchContainer.contains(e.target)) hideSearchHistory();
});

clearSearchBtn.addEventListener("click", () => {
  searchInput.value = "";
  dealsData = [];
  currentPage = 1;
  isSearchMode = false;
  savedSearchQuery = '';
  hasMoreData = true;
  clearSearchBtn.classList.add("hidden");
  container.innerHTML = "";
  prevBtn.disabled = true;
  nextBtn.disabled = true;
  if (filterSortArea) filterSortArea.classList.remove("hidden");
  localStorage.removeItem('searchResults');
  localStorage.removeItem('searchQuery');
  localStorage.removeItem('isSearchMode');
});

// --- データ取得・レンダリング ---
fetchBtn.addEventListener("click", async () => {
  selectedStores = Array.from(document.querySelectorAll(".store-checkbox:checked")).map(cb => cb.value).join(",");
  if (!selectedStores) { alert("少なくとも1つのストアを選択してください。"); return; }
  if (isFetching) return;

  isFetching = true;
  fetchBtn.disabled = true;
  fetchBtn.textContent = "検索中...";

  dealsData = [];
  currentPage = 1;
  totalFetched = 0;
  hasMoreData = true;
  isSearchMode = false;
  savedSearchQuery = '';
  clearSearchBtn.classList.add("hidden");
  if (filterSortArea) filterSortArea.classList.remove("hidden");

  currentSort = sortSelect.value || "default";
  await fetchMoreDeals();
  renderPage();

  fetchBtn.disabled = false;
  fetchBtn.textContent = "🔍 セール情報を取得";
  isFetching = false;
});

async function fetchMoreDeals() {
  if (!hasMoreData) return;
  let sortParam = "default";
  if (currentSort === "priceNewAsc") sortParam = "price";
  else if (currentSort === "priceNewDesc") sortParam = "-price";
  else if (currentSort === "cutAsc") sortParam = "cut";
  else if (currentSort === "cutDesc") sortParam = "-cut";

  const url = `/api/deals?stores=${selectedStores}&offset=${totalFetched}&limit=200&sort=${sortParam}`;
  console.log("[API] Fetching URL:", url);

  const res = await fetch(url);
  const newDeals = await res.json();

  if (!newDeals || newDeals.length === 0) {
    hasMoreData = false;
    return;
  }
  dealsData.push(...newDeals);
  totalFetched += newDeals.length;
  if (newDeals.length < 200) hasMoreData = false;
}

function isDLC(title) {
  if (!title) return false;
  const normalized = title.toLowerCase().replace(/[Ａ-Ｚａ-ｚ０-９]/g, s => String.fromCharCode(s.charCodeAt(0) - 0xFEE0)).replace(/\s+/g, "");
  const dlcPatterns = [/dlc/, /soundtracks?/, /ost/, /seasonpass/, /expansion/, /upgrade/, /addon/, /add[-\s]?on/, /pack/, /bundle/, /パック/, /拡張/, /追加コンテンツ/, /サウンドトラック/];
  return dlcPatterns.some(pattern => pattern.test(normalized));
}

function isExcludedShop(shopName) {
  if (!shopName) return false;
  return shopName.toLowerCase().includes("indiegala");
}

function hasSale(deal) {
  return Number(deal?.cut || 0) > 0;
}

function toNumber(value) {
  if (value === null || value === undefined) return NaN;
  if (typeof value === "number") return value;
  // "1,234" / "1234円" みたいな表記ゆれ対策
  const normalized = String(value).replace(/[,，\s]|円/g, "");
  const num = Number(normalized);
  return Number.isFinite(num) ? num : NaN;
}

// IndieGala除外で「表示用の最安値」を計算（モーダルは元のdealのまま）
function getCheapestNonExcludedDealForCard(deal) {
  const candidates = [];

  if (deal && !isExcludedShop(deal.shop)) candidates.push(deal);
  if (Array.isArray(deal?.otherDeals)) {
    for (const od of deal.otherDeals) {
      if (od && !isExcludedShop(od.shop)) candidates.push(od);
    }
  }

  const valid = candidates
    .map(d => ({ deal: d, price: toNumber(d.priceNew) }))
    .filter(x => Number.isFinite(x.price));

  if (valid.length === 0) return null;
  valid.sort((a, b) => a.price - b.price);
  return valid[0].deal;
}

function applyLocalSort(list) {
  if (currentSort === "priceOldAsc") return list.sort((a, b) => (a.priceOld || 0) - (b.priceOld || 0));
  if (currentSort === "priceOldDesc") return list.sort((a, b) => (b.priceOld || 0) - (a.priceOld || 0));
  return list;
}

function renderPage() {
  container.innerHTML = "";
  let filteredDeals = excludeDLC ? dealsData.filter(d => !isDLC(d.title)) : [...dealsData];
  filteredDeals = applyLocalSort(filteredDeals);

  const totalPages = Math.ceil(filteredDeals.length / itemsPerPage);
  if (currentPage > totalPages) currentPage = totalPages || 1;

  const start = (currentPage - 1) * itemsPerPage;
  const pageItems = filteredDeals.slice(start, start + itemsPerPage);

  pageItems.forEach((deal, index) => {
    const displayDeal = getCheapestNonExcludedDealForCard(deal);
    // IndieGalaしか無い等、表示できるストアが無い場合はカード自体を出さない
    if (!displayDeal) return;

    const card = document.createElement("div");
    const cardClass = darkMode ? "bg-gray-800/80 text-gray-100 hover:bg-gray-700/80 border border-cyan-500/20" : "bg-white text-gray-800 hover:shadow-2xl border border-blue-200";
    card.className = `${cardClass} p-5 rounded-2xl shadow-lg transition-all duration-500 opacity-0 translate-y-4 cursor-pointer backdrop-blur-sm`;
    card.dataset.gameId = deal.gameID || deal.id || deal.game?.id;

    const img = deal.image && deal.image.trim() !== "" ? deal.image : "https://placehold.co/400x185?text=No+Image";
    const textColorClass = darkMode ? "text-gray-400" : "text-gray-600";
    
    const sale = hasSale(displayDeal);

    // セール時のみ終了日時を表示（不明表示は出さない）
    let expiryText = '';
    if (sale && displayDeal.expiry) {
      try {
        const expiryDate = new Date(displayDeal.expiry);
        const now = new Date();
        const diffTime = expiryDate - now;
        const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        const expiryDateStr = expiryDate.toLocaleDateString('ja-JP', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        });

        if (diffHours < 0) {
          expiryText = `<p class="text-sm text-red-500 font-semibold mt-1">⏰ セール終了済み (${expiryDateStr})</p>`;
        } else if (diffHours < 24) {
          expiryText = `<p class="text-sm text-red-500 font-semibold mt-1 animate-pulse">⏰ 残り ${diffHours}時間 (${expiryDateStr})</p>`;
        } else if (diffDays <= 3) {
          expiryText = `<p class="text-sm text-orange-500 font-semibold mt-1">⏰ 残り ${diffDays}日 (${expiryDateStr})</p>`;
        } else if (diffDays <= 7) {
          expiryText = `<p class="text-sm text-yellow-500 font-semibold mt-1">⏰ 残り ${diffDays}日 (${expiryDateStr})</p>`;
        } else {
          expiryText = `<p class="text-sm ${textColorClass} mt-1">⏰ ${expiryDateStr}まで</p>`;
        }
      } catch (e) {
        console.error('Error parsing expiry date:', e);
      }
    }
    
    card.innerHTML = `
      <img src="${img}" class="w-full rounded-xl mb-3 shadow-md" alt="thumbnail">
      <h2 class="font-bold text-lg mb-2 line-clamp-2">${deal.title}</h2>
      <p class="text-sm ${textColorClass} mb-1">🏪 ${displayDeal.shop}</p>
      ${sale ? `<p class="text-sm ${textColorClass}">通常: <span class="line-through">${displayDeal.priceOld}円</span></p>` : ``}
      <p class="text-red-500 font-bold text-xl my-2">最安値: ${displayDeal.priceNew}円</p>
      ${sale ? `<p class="text-sm text-green-500 font-semibold">💰 ${displayDeal.cut}% OFF</p>` : ``}
      ${expiryText}
    `;

    card.addEventListener("click", async () => {
      const gameId = card.dataset.gameId;
      if (!gameId) return;

      await openModal(deal);
    });
    container.appendChild(card);
    setTimeout(() => {
      card.classList.remove("opacity-0", "translate-y-4");
      card.classList.add("opacity-100", "translate-y-0");
    }, index * 10);
  });

  prevBtn.disabled = currentPage === 1;
  nextBtn.disabled = currentPage >= totalPages && !hasMoreData;
  window.scrollTo({ top: 0, behavior: "smooth" });
}

// --- モーダル制御 ---
async function openModal(deal) {
  currentDeal = deal;
  const modal = document.getElementById("gameModal");
  const modalContent = document.getElementById("modalContent");
  const storeLinkContainer = document.getElementById("storeLinkContainer");
  
  const loadingColor = darkMode ? "text-gray-400" : "text-gray-500";
  modalContent.innerHTML = `<p class="text-center ${loadingColor}">詳細を取得中...</p>`;
  storeLinkContainer.classList.add("hidden");
  modal.classList.remove("hidden");
  modal.classList.add("flex");

  try {
    const res = await fetch(`/api/gameinfo?id=${deal.gameID || deal.id}`);
    const data = await res.json();
    const bgClass = darkMode ? "bg-gray-700/50 backdrop-blur-sm" : "bg-gray-50";
    const borderClass = darkMode ? "border-gray-600" : "border-gray-300";
    // モーダル内でもIndieGalaを表示しない（表示用の最安値を再計算）
    const modalDeal = getCheapestNonExcludedDealForCard(deal);
    const sale = hasSale(modalDeal);
    
    // ストアページとウィッシュリスト追加ボタン
    let buttonsHtml = '';
    if (modalDeal?.url) {
      buttonsHtml = `
        <div class="flex gap-3 justify-center mb-4">
          <a href="${modalDeal.url}" target="_blank" 
             class="bg-gradient-to-r from-blue-500 to-blue-600 text-white px-6 py-3 rounded-lg hover:shadow-xl transition-all font-semibold">
            🔗 ストアページ
          </a>
          <button id="addToWishlistBtnModal"
                  class="bg-gradient-to-r from-yellow-500 to-yellow-600 text-white px-6 py-3 rounded-lg hover:shadow-xl transition-all font-semibold">
            ⭐ ウィッシュリスト追加
          </button>
        </div>
      `;
    }
    
    // 最安値ストアの情報（IndieGala除外後）
    let modalHtml = `
      <img src="${data.assets?.banner400 || deal.image}" class="rounded-2xl w-full mb-4 shadow-lg">
      <h2 class="text-2xl font-bold mb-3">${data.title || deal.title}</h2>
      
      ${buttonsHtml}
      
      <div class="${bgClass} p-4 rounded-xl mt-3 space-y-2 border ${borderClass}">
        <h3 class="text-lg font-semibold mb-2 text-green-500">💰 最安値ストア</h3>
        ${modalDeal ? `
          <p class="flex items-center justify-between"><span>ストア</span><span class="font-bold">${modalDeal.shop}</span></p>
          <p class="flex items-center justify-between"><span>${sale ? 'セール価格' : '価格'}</span><span class="font-bold text-xl text-red-500">${modalDeal.priceNew}円</span></p>
          ${sale ? `<p class="flex items-center justify-between text-sm ${darkMode ? 'text-gray-400' : 'text-gray-600'}"><span>通常価格</span><span class="line-through">${modalDeal.priceOld}円</span></p>` : ``}
          ${sale ? `<p class="flex items-center justify-between"><span class="text-green-500">割引率</span><span class="font-bold text-green-500">${modalDeal.cut}% OFF</span></p>` : ``}
        ` : `
          <p class="text-sm opacity-70">IndieGala以外のストア情報が見つかりませんでした。</p>
        `}
        ${sale && modalDeal?.expiry ? (() => {
          try {
            const expiryDate = new Date(modalDeal.expiry);
            const now = new Date();
            const diffTime = expiryDate - now;
            const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            const expiryDateStr = expiryDate.toLocaleDateString('ja-JP', { 
              year: 'numeric', 
              month: 'long', 
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit'
            });
            
            if (diffHours < 0) {
              return `<p class="text-sm mb-1 text-red-500 font-semibold"><span class="font-semibold">⏰ セール:</span> 終了済み (${expiryDateStr})</p>`;
            } else if (diffHours < 24) {
              return `<p class="text-sm mb-1 text-red-500 font-semibold animate-pulse"><span class="font-semibold">⏰ セール終了:</span> 残り ${diffHours}時間 (${expiryDateStr})</p>`;
            } else if (diffDays <= 3) {
              return `<p class="text-sm mb-1 text-orange-500 font-semibold"><span class="font-semibold">⏰ セール終了:</span> ${expiryDateStr} (残り${diffDays}日)</p>`;
            } else {
              return `<p class="text-sm mb-1"><span class="font-semibold">⏰ セール終了:</span> ${expiryDateStr}</p>`;
            }
          } catch (e) {
            return '';
          }
        })() : ''}
        ${deal.historyLow ? `<p class="flex items-center justify-between text-sm ${darkMode ? 'text-gray-400' : 'text-gray-600'}"><span>🕒 過去最安値</span><span>${deal.historyLow}円</span></p>` : ''}
      </div>
    `;

    // 他のストアの情報を表示
    if (deal.otherDeals && deal.otherDeals.length > 0) {
      modalHtml += `
        <div class="${bgClass} p-4 rounded-xl border ${borderClass} mt-4">
          <h3 class="text-lg font-semibold mb-3">🏪 他のストアの価格</h3>
          <div class="space-y-3">
      `;
      
      deal.otherDeals.forEach((otherDeal, idx) => {
        if (isExcludedShop(otherDeal.shop)) return;
        const otherSale = hasSale(otherDeal);
        let expiryHtml = '';
        if (otherSale && otherDeal.expiry) {
          try {
            const expiryDate = new Date(otherDeal.expiry);
            const now = new Date();
            const diffTime = expiryDate - now;
            const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            const expiryDateStr = expiryDate.toLocaleDateString('ja-JP', { 
              year: 'numeric', 
              month: 'long', 
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit'
            });
            
            if (diffHours < 0) {
              expiryHtml = `<p class="text-sm mb-1 text-red-500 font-semibold">⏰ 終了済み (${expiryDateStr})</p>`;
            } else if (diffHours < 24) {
              expiryHtml = `<p class="text-sm mb-1 text-red-500 font-semibold animate-pulse">⏰ 残り ${diffHours}時間 (${expiryDateStr})</p>`;
            } else if (diffDays <= 3) {
              expiryHtml = `<p class="text-sm mb-1 text-orange-500 font-semibold">⏰ ${expiryDateStr} (残り${diffDays}日)</p>`;
            } else {
              expiryHtml = `<p class="text-sm mb-1">⏰ ${expiryDateStr}</p>`;
            }
          } catch (e) {
            // エラー時は何も表示しない
          }
        }
        
        modalHtml += `
          <div class="border-b ${borderClass} pb-3 last:border-b-0">
            <p class="text-sm mb-1"><span class="font-semibold">${otherDeal.shop}</span></p>
            <p class="text-sm mb-1">${otherSale ? 'セール価格' : '価格'}: <span class="text-red-500 font-bold">${otherDeal.priceNew}円</span> ${otherSale ? `<span class="text-green-500">(${otherDeal.cut}%OFF)</span>` : ''}</p>
            ${otherSale ? `<p class="text-sm mb-1">通常価格: <span class="line-through">${otherDeal.priceOld}円</span></p>` : ``}
            ${expiryHtml}
            <div class="flex items-center justify-between gap-2 mt-1">
              ${otherDeal.url ? `<a href="${otherDeal.url}" target="_blank" class="text-blue-500 hover:underline text-xs inline-block">ストアページへ →</a>` : '<span class="text-xs opacity-60">リンクなし</span>'}
              <button class="addToWishlistStoreBtn text-xs px-3 py-1 rounded-lg bg-gradient-to-r from-yellow-500 to-yellow-600 text-white font-semibold"
                      data-store-index="${idx}">
                ⭐ このストアで追加
              </button>
            </div>
          </div>
        `;
      });
      
      modalHtml += `
          </div>
        </div>
      `;
    }

    modalContent.innerHTML = modalHtml;

    // 他ストアの「このストアで追加」
    modalContent.querySelectorAll(".addToWishlistStoreBtn").forEach(btn => {
      btn.addEventListener("click", async () => {
        if (!currentDeal) return;
        const idx = Number(btn.dataset.storeIndex);
        const target = Array.isArray(currentDeal.otherDeals) ? currentDeal.otherDeals[idx] : null;
        if (!target || isExcludedShop(target.shop)) return;

        const gameId = currentDeal.gameID || currentDeal.id;
        const payload = {
          gameId,
          gameTitle: currentDeal.title,
          gameImage: currentDeal.image,
          currentPrice: target.priceNew,
          shop: target.shop,
          url: target.url,
          priceOld: target.priceOld,
          cut: target.cut,
          expiry: target.expiry,
          historyLow: currentDeal.historyLow,
          historyLow1y: currentDeal.historyLow1y,
          historyLow3m: currentDeal.historyLow3m,
          storeLow: currentDeal.storeLow
        };

        try {
          const res = await fetch("/api/wishlist", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
          });
          const result = await res.json();
          showToast(result.success ? "ウィッシュリストに追加しました！" : result.message, result.success);
        } catch (err) {
          console.error(err);
          showToast("❌ エラーが発生しました", false);
        }
      });
    });

    // ウィッシュリスト追加ボタンのイベントリスナーを設定
    const addToWishlistBtnModal = document.getElementById("addToWishlistBtnModal");
    if (addToWishlistBtnModal) {
      addToWishlistBtnModal.addEventListener("click", async () => {
        if (!currentDeal) return;

        const gameId = currentDeal.gameID || currentDeal.id;
        const wishlistDeal = getCheapestNonExcludedDealForCard(currentDeal);
        if (!wishlistDeal) {
          showToast("追加できませんでした", false);
          return;
        }
        const data = {
          gameId: gameId,
          gameTitle: currentDeal.title,
          gameImage: currentDeal.image,
          currentPrice: wishlistDeal.priceNew,
          shop: wishlistDeal.shop,
          url: wishlistDeal.url,
          priceOld: wishlistDeal.priceOld,
          cut: wishlistDeal.cut,
          expiry: wishlistDeal.expiry,
          historyLow: currentDeal.historyLow,
          historyLow1y: currentDeal.historyLow1y,
          historyLow3m: currentDeal.historyLow3m,
          storeLow: currentDeal.storeLow
        };

        try {
          const res = await fetch("/api/wishlist", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
          });

          const result = await res.json();
          
          if (result.success) {
            showToast("ウィッシュリストに追加しました！", true);
          } else {
            showToast(result.message, false);
          }
        } catch (err) {
          console.error(err);
          showToast("❌ エラーが発生しました", false);
        }
      });
    }
  } catch (err) {
    console.error(err);
    modalContent.innerHTML = `<p class="text-center ${darkMode ? 'text-gray-400' : 'text-gray-500'}">情報の取得に失敗しました。</p>`;
  }
}

// 下部のウィッシュリスト追加ボタン (既存のaddToWishlistBtn も同様に修正)
const addToWishlistBtn = document.getElementById("addToWishlistBtn");
if (addToWishlistBtn) {
addToWishlistBtn.addEventListener("click", async () => {
  if (!currentDeal) return;

  const gameId = currentDeal.gameID || currentDeal.id;
  const data = {
    gameId: gameId,
    gameTitle: currentDeal.title,
    gameImage: currentDeal.image,
    currentPrice: currentDeal.priceNew,
      priceOld: currentDeal.priceOld,
      cut: currentDeal.cut,
    shop: currentDeal.shop,
      url: currentDeal.url,
      // 過去最安値情報を追加
      historyLow: currentDeal.historyLow,
      historyLow1y: currentDeal.historyLow1y,
      historyLow3m: currentDeal.historyLow3m,
      storeLow: currentDeal.storeLow,
      // セール終了日時を追加
      expiry: currentDeal.expiry
  };

  try {
    const res = await fetch("/api/wishlist", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    });

    const result = await res.json();
    
    if (result.success) {
          showToast("ウィッシュリストに追加しました!", true);
    } else {
          showToast(result.message, false);
    }
  } catch (err) {
    console.error(err);
        showToast("❌ エラーが発生しました", false);
      }
  });
}

// --- ページ初期化 ---
window.addEventListener("DOMContentLoaded", async () => {
  await loadSearchHistory();
  const savedResults = localStorage.getItem('searchResults');
  const savedQuery = localStorage.getItem('searchQuery');
  const savedIsSearchMode = localStorage.getItem('isSearchMode');
  
  if (savedResults && savedIsSearchMode === 'true') {
    try {
      dealsData = JSON.parse(savedResults);
      isSearchMode = true;
      savedSearchQuery = savedQuery || '';
      hasMoreData = false;
      
      if (searchInput && savedQuery) {
        searchInput.value = savedQuery;
      }
      
      if (clearSearchBtn) {
        clearSearchBtn.classList.remove("hidden");
      }
      
      if (filterSortArea) {
        filterSortArea.classList.add("hidden");
      }
      
      currentPage = 1;
      renderPage();
      console.log("[Search] Restored search results from localStorage");
    } catch (e) {
      console.error("[Search] Error restoring search results:", e);
      localStorage.removeItem('searchResults');
      localStorage.removeItem('searchQuery');
      localStorage.removeItem('isSearchMode');
    }
  }
});

// 各種ボタンイベント
sortSelect.addEventListener("change", () => {
  currentSort = sortSelect.value;
  totalFetched = 0;
  dealsData = [];
  fetchMoreDeals().then(renderPage);
});

prevBtn.addEventListener("click", () => { if (currentPage > 1) { currentPage--; renderPage(); } });
nextBtn.addEventListener("click", async () => {
  const filtered = excludeDLC ? dealsData.filter(d => !isDLC(d.title)) : dealsData;
  if (currentPage < Math.ceil(filtered.length / itemsPerPage)) {
    currentPage++; renderPage();
  } else if (hasMoreData && !isSearchMode) {
    await fetchMoreDeals(); renderPage();
  }
});

const closeModal = document.getElementById("closeModal");
const modal = document.getElementById("gameModal");
closeModal.addEventListener("click", () => {
  modal.classList.add("hidden");
  modal.classList.remove("flex");
});
modal.addEventListener("click", (e) => {
  if (e.target === modal) {
    modal.classList.add("hidden");
    modal.classList.remove("flex");
  }
});
scrollTopBtn.addEventListener("click", () => window.scrollTo({ top: 0, behavior: "smooth" }));
window.addEventListener("scroll", () => scrollTopBtn.style.display = window.scrollY > 200 ? "block" : "none");

// トースト通知表示関数
function showToast(message, isSuccess = true) {
  const toast = document.getElementById("toast");
  const toastContent = document.getElementById("toastContent");
  const toastIcon = document.getElementById("toastIcon");
  const toastMessage = document.getElementById("toastMessage");
  const toastClose = document.getElementById("toastClose");
  
  if (!toast || !toastContent || !toastIcon || !toastMessage || !toastClose) return;
  
  // メッセージとアイコンを設定
  toastMessage.textContent = message;
  toastIcon.textContent = isSuccess ? "✅" : "⚠️";
  
  // スタイルを設定
  const bgClass = darkMode 
    ? (isSuccess ? "bg-gray-800 text-gray-100" : "bg-gray-800 text-gray-100")
    : (isSuccess ? "bg-white text-gray-800" : "bg-white text-gray-800");
  const borderColor = isSuccess ? "border-green-500" : "border-yellow-500";
  
  toastContent.className = `rounded-lg shadow-lg p-4 border-l-4 min-w-[300px] transition-all duration-300 ${bgClass} ${borderColor}`;
  
  // 表示
  toast.classList.remove("hidden");
  toast.style.opacity = "0";
  toast.style.transform = "translateX(100%)";
  
  // アニメーション
  setTimeout(() => {
    toast.style.transition = "all 0.3s ease-out";
    toast.style.opacity = "1";
    toast.style.transform = "translateX(0)";
  }, 10);
  
  // 閉じるボタンのイベント
  const closeToast = () => {
    toast.style.opacity = "0";
    toast.style.transform = "translateX(100%)";
    setTimeout(() => {
      toast.classList.add("hidden");
    }, 300);
  };
  
  toastClose.onclick = closeToast;
  
  // 3秒後に自動で閉じる
  setTimeout(closeToast, 3000);
}