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

const excludeDLCCheckbox = document.getElementById("excludeDLC");
if (excludeDLCCheckbox) {
  excludeDLC = excludeDLCCheckbox.checked;
  excludeDLCCheckbox.addEventListener("change", () => {
    excludeDLC = excludeDLCCheckbox.checked;
    currentPage = 1;
    renderPage();
  });
}

// 検索機能
searchBtn.addEventListener("click", async () => {
  const query = searchInput.value.trim();
  if (!query) {
    alert("検索キーワードを入力してください。");
    return;
  }

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
    dealsData = [];
  } finally {
    isFetching = false;
    searchBtn.disabled = false;
    searchBtn.textContent = "🔍 検索";
    loadingMessage.classList.add("hidden");
    renderPage();
  }
});

// Enterキーで検索
searchInput.addEventListener("keypress", (e) => {
  if (e.key === "Enter") {
    searchBtn.click();
  }
});

// 検索クリア
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
  
  localStorage.removeItem('searchResults');
  localStorage.removeItem('searchQuery');
  localStorage.removeItem('isSearchMode');
});

fetchBtn.addEventListener("click", async () => {
  selectedStores = Array.from(document.querySelectorAll(".store-checkbox:checked"))
    .map(cb => cb.value)
    .join(",");

  if (!selectedStores) {
    alert("少なくとも1つのストアを選択してください。");
    return;
  }

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
  searchInput.value = "";

  localStorage.removeItem('searchResults');
  localStorage.removeItem('searchQuery');
  localStorage.removeItem('isSearchMode');

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

  switch (currentSort) {
    case "priceNewAsc": sortParam = "price"; break;
    case "priceNewDesc": sortParam = "-price"; break;
    case "cutAsc": sortParam = "cut"; break;
    case "cutDesc": sortParam = "-cut"; break;
    case "priceOldAsc":
    case "priceOldDesc":
      sortParam = "default";
      break;
    default:
      sortParam = "default";
  }

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
  const normalized = title
    .toLowerCase()
    .replace(/[Ａ-Ｚａ-ｚ０-９]/g, s => String.fromCharCode(s.charCodeAt(0) - 0xFEE0))
    .replace(/\s+/g, "");
  const dlcPatterns = [
    /dlc/, /soundtracks?/, /ost/,
    /seasonpass/, /expansion/,
    /upgrade/, /addon/, /add[-\s]?on/,
    /pack/, /bundle/,
    /パック/, /拡張/, /追加コンテンツ/,
    /サウンドトラック/
  ];
  return dlcPatterns.some(pattern => pattern.test(normalized));
}

function applyLocalSort(list) {
  switch (currentSort) {
    case "priceOldAsc":
      return list.sort((a, b) => (a.priceOld || 0) - (b.priceOld || 0));
    case "priceOldDesc":
      return list.sort((a, b) => (b.priceOld || 0) - (a.priceOld || 0));
  }
  return list;
}

function renderPage() {
  container.innerHTML = "";

  let filteredDeals = excludeDLC ? dealsData.filter(d => !isDLC(d.title)) : [...dealsData];
  filteredDeals = applyLocalSort(filteredDeals);

  const totalPages = Math.ceil(filteredDeals.length / itemsPerPage);
  if (currentPage > totalPages) currentPage = totalPages;

  const start = (currentPage - 1) * itemsPerPage;
  const end = start + itemsPerPage;
  const pageItems = filteredDeals.slice(start, end);

  pageItems.forEach((deal, index) => {
    const card = document.createElement("div");
    const cardClass = darkMode 
      ? "bg-gray-800/80 text-gray-100 hover:bg-gray-700/80 border border-cyan-500/20" 
      : "bg-white text-gray-800 hover:shadow-2xl border border-blue-200";
    
    card.className = `${cardClass} p-5 rounded-2xl shadow-lg transition-all duration-500 opacity-0 translate-y-4 cursor-pointer backdrop-blur-sm`;
    card.dataset.gameId = deal.gameID || deal.id || deal.game?.id;

    const img = deal.image && deal.image.trim() !== ""
      ? deal.image
      : "https://placehold.co/400x185?text=No+Image";

    const textColorClass = darkMode ? "text-gray-400" : "text-gray-600";
    
    // セール終了日時の表示用テキストを生成
    let expiryText = '';
    if (deal.expiry) {
      try {
        const expiryDate = new Date(deal.expiry);
        const now = new Date();
        const diffTime = expiryDate - now;
        const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        
        if (diffHours < 0) {
          expiryText = `<p class="text-sm text-red-500 font-semibold mt-1">⏰ セール終了済み</p>`;
        } else if (diffHours < 24) {
          expiryText = `<p class="text-sm text-red-500 font-semibold mt-1 animate-pulse">⏰ 残り ${diffHours}時間</p>`;
        } else if (diffDays <= 3) {
          expiryText = `<p class="text-sm text-orange-500 font-semibold mt-1">⏰ 残り ${diffDays}日</p>`;
        } else if (diffDays <= 7) {
          expiryText = `<p class="text-sm text-yellow-500 font-semibold mt-1">⏰ 残り ${diffDays}日</p>`;
        } else {
          const expiryDateStr = expiryDate.toLocaleDateString('ja-JP', { 
            month: 'numeric', 
            day: 'numeric'
          });
          expiryText = `<p class="text-sm ${textColorClass} mt-1">⏰ ${expiryDateStr}まで</p>`;
        }
      } catch (e) {
        console.error('Error parsing expiry date:', e);
      }
    } else {
      // 終了日時不明の場合
      expiryText = `<p class="text-sm ${textColorClass} mt-1 opacity-60">⏰ 終了日不明</p>`;
    }
    
    card.innerHTML = `
      <img src="${img}" class="w-full rounded-xl mb-3 shadow-md" alt="thumbnail">
      <h2 class="font-bold text-lg mb-2 line-clamp-2">${deal.title}</h2>
      <p class="text-sm ${textColorClass} mb-1">🏪 ${deal.shop}</p>
      <p class="text-sm ${textColorClass}">通常: <span class="line-through">${deal.priceOld}円</span></p>
      <p class="text-red-500 font-bold text-xl my-2">セール: ${deal.priceNew}円</p>
      <p class="text-sm text-green-500 font-semibold">💰 ${deal.cut}% OFF</p>
      ${expiryText}
    `;
    container.appendChild(card);

    card.addEventListener("click", async () => {
      const gameId = card.dataset.gameId;
      if (!gameId) return;

      currentDeal = deal;

      try {
        const modal = document.getElementById("gameModal");
        const modalContent = document.getElementById("modalContent");
        const storeLinkContainer = document.getElementById("storeLinkContainer");

        const loadingColor = darkMode ? "text-gray-400" : "text-gray-500";
        modalContent.innerHTML = `<p class="text-center ${loadingColor}">詳細を取得中...</p>`;
        storeLinkContainer.classList.add("hidden");
        modal.classList.remove("hidden");
        modal.classList.add("flex");

        const res = await fetch(`/api/gameinfo?id=${gameId}`);
        const data = await res.json();

        const bgClass = darkMode ? "bg-gray-700/50 backdrop-blur-sm" : "bg-gray-50";
        const borderClass = darkMode ? "border-gray-600" : "border-gray-300";
        
        // ストアページとウィッシュリスト追加ボタン
        let buttonsHtml = '';
        if (deal.url) {
          buttonsHtml = `
            <div class="flex gap-3 justify-center mb-4">
              <a href="${deal.url}" target="_blank" 
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
        
        // 最安値ストアの情報
        let modalHtml = `
          <img src="${data.assets?.banner400 || deal.image}" class="rounded-2xl w-full mb-4 shadow-lg">
          <h2 class="text-2xl font-bold mb-3">${data.title || deal.title}</h2>
          
          ${buttonsHtml}
          
          <div class="${bgClass} p-4 rounded-xl mt-3 space-y-2 border ${borderClass}">
            <h3 class="text-lg font-semibold mb-2 text-green-500">💰 最安値ストア</h3>
            <p class="flex items-center justify-between"><span>ストア</span><span class="font-bold">${deal.shop}</span></p>
            <p class="flex items-center justify-between"><span>セール価格</span><span class="font-bold text-xl text-red-500">${deal.priceNew}円</span></p>
            <p class="flex items-center justify-between text-sm ${darkMode ? 'text-gray-400' : 'text-gray-600'}"><span>通常価格</span><span class="line-through">${deal.priceOld}円</span></p>
            <p class="flex items-center justify-between"><span class="text-green-500">割引率</span><span class="font-bold text-green-500">${deal.cut}% OFF</span></p>
            ${deal.expiry ? (() => {
              try {
                const expiryDate = new Date(deal.expiry);
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
                  return `<p class="text-sm mb-1 text-red-500 font-semibold"><span class="font-semibold">⏰ セール:</span> 終了済み</p>`;
                } else if (diffHours < 24) {
                  return `<p class="text-sm mb-1 text-red-500 font-semibold animate-pulse"><span class="font-semibold">⏰ セール終了:</span> 残り ${diffHours}時間</p>`;
                } else if (diffDays <= 3) {
                  return `<p class="text-sm mb-1 text-orange-500 font-semibold"><span class="font-semibold">⏰ セール終了:</span> ${expiryDateStr} (残り${diffDays}日)</p>`;
                } else {
                  return `<p class="text-sm mb-1"><span class="font-semibold">⏰ セール終了:</span> ${expiryDateStr}</p>`;
                }
              } catch (e) {
                return '';
              }
            })() : '<p class="text-sm mb-1 opacity-60"><span class="font-semibold">⏰ セール終了:</span> 不明（ストアで確認）</p>'}
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
          
          deal.otherDeals.forEach(otherDeal => {
            let expiryHtml = '';
            if (otherDeal.expiry) {
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
                  expiryHtml = `<p class="text-sm mb-1 text-red-500 font-semibold">⏰ 終了済み</p>`;
                } else if (diffHours < 24) {
                  expiryHtml = `<p class="text-sm mb-1 text-red-500 font-semibold animate-pulse">⏰ 残り ${diffHours}時間</p>`;
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
                <p class="text-sm mb-1">セール価格: <span class="text-red-500 font-bold">${otherDeal.priceNew}円</span> <span class="text-green-500">(${otherDeal.cut}%OFF)</span></p>
                <p class="text-sm mb-1">通常価格: <span class="line-through">${otherDeal.priceOld}円</span></p>
                ${expiryHtml}
                ${otherDeal.url ? `<a href="${otherDeal.url}" target="_blank" class="text-blue-500 hover:underline text-xs mt-1 inline-block">ストアページへ →</a>` : ''}
              </div>
            `;
          });
          
          modalHtml += `
              </div>
            </div>
          `;
        }

        modalContent.innerHTML = modalHtml;

// モーダル内のウィッシュリスト追加ボタン
const addToWishlistBtnModal = document.getElementById("addToWishlistBtnModal");
if (addToWishlistBtnModal) {
  addToWishlistBtnModal.addEventListener("click", async () => {
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
        alert("✅ ウィッシュリストに追加しました!");
      } else {
        alert("⚠️ " + result.message);
      }
    } catch (err) {
      console.error(err);
      alert("❌ エラーが発生しました");
    }
  });
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
        alert("✅ ウィッシュリストに追加しました!");
      } else {
        alert("⚠️ " + result.message);
      }
    } catch (err) {
      console.error(err);
      alert("❌ エラーが発生しました");
    }
  });
}

      } catch (err) {
        console.error(err);
      }
    });

    setTimeout(() => {
      card.classList.remove("opacity-0", "translate-y-4");
      card.classList.add("opacity-100", "translate-y-0");
    }, index * 10);
  });

  prevBtn.disabled = currentPage === 1;
  nextBtn.disabled = currentPage >= totalPages && !hasMoreData;
  window.scrollTo({ top: 0, behavior: "smooth" });
}

sortSelect.addEventListener("change", async () => {
  currentSort = sortSelect.value || "default";
  dealsData = [];
  currentPage = 1;
  totalFetched = 0;
  hasMoreData = true;

  await fetchMoreDeals();
  renderPage();
});

prevBtn.addEventListener("click", () => {
  if (currentPage > 1) {
    currentPage--;
    renderPage();
  }
});

nextBtn.addEventListener("click", async () => {
  let filteredDeals = excludeDLC ? dealsData.filter(d => !isDLC(d.title)) : [...dealsData];
  filteredDeals = applyLocalSort(filteredDeals);

  const totalPages = Math.ceil(filteredDeals.length / itemsPerPage);

  if (currentPage < totalPages) {
    currentPage++;
    renderPage();
  } else if (hasMoreData && !isSearchMode) {
    await fetchMoreDeals();
    renderPage();
  }
});

window.addEventListener("scroll", () => {
  scrollTopBtn.style.display = window.scrollY > 200 ? "block" : "none";
});

scrollTopBtn.addEventListener("click", () => {
  window.scrollTo({ top: 0, behavior: "smooth" });
});

const modal = document.getElementById("gameModal");
const closeModal = document.getElementById("closeModal");

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
      shop: currentDeal.shop,
      url: currentDeal.url,
      priceOld: currentDeal.priceOld,
      cut: currentDeal.cut,
      expiry: currentDeal.expiry,
      historyLow: currentDeal.historyLow
    };

    try {
      const res = await fetch("/api/wishlist", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
      });

      const result = await res.json();
      
      if (result.success) {
        alert("✅ ウィッシュリストに追加しました！");
      } else {
        alert("⚠️ " + result.message);
      }
    } catch (err) {
      console.error(err);
      alert("❌ エラーが発生しました");
    }
  });
}

// ページ読み込み時に検索結果を復元
window.addEventListener("DOMContentLoaded", () => {
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