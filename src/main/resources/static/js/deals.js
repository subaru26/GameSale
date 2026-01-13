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

const container = document.getElementById("dealsContainer");
const prevBtn = document.getElementById("prevPage");
const nextBtn = document.getElementById("nextPage");
const scrollTopBtn = document.getElementById("scrollTopBtn");
const fetchBtn = document.getElementById("fetchDealsBtn");
const sortSelect = document.getElementById("sortSelect");

const excludeDLCCheckbox = document.getElementById("excludeDLC");
if (excludeDLCCheckbox) {
  excludeDLC = excludeDLCCheckbox.checked;
  excludeDLCCheckbox.addEventListener("change", () => {
    excludeDLC = excludeDLCCheckbox.checked;
    currentPage = 1;
    renderPage();
  });
}

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

  currentSort = sortSelect.value || "default";

  await fetchMoreDeals();
  renderPage();

  fetchBtn.disabled = false;
  fetchBtn.textContent = "セール情報を取得";
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
    
    card.innerHTML = `
      <img src="${img}" class="w-full rounded-xl mb-3 shadow-md" alt="thumbnail">
      <h2 class="font-bold text-lg mb-2 line-clamp-2">${deal.title}</h2>
      <p class="text-sm ${textColorClass} mb-1">🏪 ${deal.shop}</p>
      <p class="text-sm ${textColorClass}">通常: <span class="line-through">${deal.priceOld}円</span></p>
      <p class="text-red-500 font-bold text-xl my-2">セール: ${deal.priceNew}円</p>
      <p class="text-sm text-green-500 font-semibold">💰 ${deal.cut}% OFF</p>
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

        const bgClass = darkMode ? "bg-gray-700" : "bg-gray-50";
        modalContent.innerHTML = `
          <img src="${data.assets?.banner400 || deal.image}" class="rounded-lg w-full mb-3">
          <h2 class="text-xl font-bold">${data.title}</h2>
          <div class="${bgClass} p-3 rounded mt-3 text-sm">
            <p>💰 現在価格: ${deal.priceNew}円 (${deal.cut}%OFF)</p>
            <p>💵 通常価格: ${deal.priceOld}円</p>
            <p>🕒 過去最安値: ${deal.historyLow || '不明'}円</p>
          </div>
        `;

        if (deal.url) {
          document.getElementById("storeLink").href = deal.url;
          storeLinkContainer.classList.remove("hidden");
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
  } else if (hasMoreData) {
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
addToWishlistBtn.addEventListener("click", async () => {
  if (!currentDeal) return;

  const gameId = currentDeal.gameID || currentDeal.id;
  const data = {
    gameId: gameId,
    gameTitle: currentDeal.title,
    gameImage: currentDeal.image,
    currentPrice: currentDeal.priceNew,
    shop: currentDeal.shop,
    url: currentDeal.url
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