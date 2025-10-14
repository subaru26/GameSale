let dealsData = [];
let currentPage = 1;
const itemsPerPage = 48;
let totalFetched = 0;
let hasMoreData = true;
let selectedStores = [];
let isFetching = false;
let excludeDLC = false;
let currentSort = "default"; // 現在のソート値

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

  // 現在選択されているソート値を取得
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

  // API用のsortパラメータに変換
  switch (currentSort) {
    case "priceNewAsc": sortParam = "price"; break;
    case "priceNewDesc": sortParam = "-price"; break;
    case "cutDesc": sortParam = "-cut"; break;
    case "cutAsc": sortParam = "cut"; break;
    default: sortParam = "default"; break;
  }

  const url = `/api/deals?stores=${selectedStores}&offset=${totalFetched}&limit=200&sort=${sortParam}`;
  console.log("[API] Fetching URL:", url); // ログ出力

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
    /dlc/,
    /soundtracks?/,
    /ost/,
    /seasonpass/,
    /expansion/,
    /upgrade/,
    /addon/,
    /add[-\s]?on/,
    /pack/,
    /bundle/,
    /expansionpass/,
    /パック/,
    /拡張/,
    /追加コンテンツ/,
    /サウンドトラック/,
    /オリジナルサウンドトラック/
  ];

  return dlcPatterns.some(pattern => pattern.test(normalized));
}

function renderPage() {
  container.innerHTML = "";

  const filteredDeals = excludeDLC ? dealsData.filter(d => !isDLC(d.title)) : dealsData;
  const totalPages = Math.ceil(filteredDeals.length / itemsPerPage);
  if (currentPage > totalPages) currentPage = totalPages;

  const start = (currentPage - 1) * itemsPerPage;
  const end = start + itemsPerPage;
  const pageItems = filteredDeals.slice(start, end);

  pageItems.forEach((deal, index) => {
    const card = document.createElement("div");
    card.className = "bg-white p-4 rounded-lg shadow transition-opacity duration-500 opacity-0 translate-y-4";
    const img = deal.image ? deal.image : "/img/no-image.png";

    card.innerHTML = `
      <img src="${img}" class="w-full rounded mb-2" alt="thumbnail">
      <h2 class="font-semibold text-lg mb-1">${deal.title}</h2>
      <p class="text-sm text-gray-600">ストア: ${deal.shop}</p>
      <p class="text-sm">通常価格: <span class="line-through">${deal.priceOld}円</span></p>
      <p class="text-red-600 font-bold">セール価格: ${deal.priceNew}円</p>
      <p class="text-sm text-green-600">割引率: ${deal.cut}%</p>
      <a href="${deal.url}" target="_blank" class="text-blue-600 underline text-sm">購入ページへ</a>
    `;
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

// ソート選択
sortSelect.addEventListener("change", async () => {
  currentSort = sortSelect.value || "default";
  dealsData = [];
  currentPage = 1;
  totalFetched = 0;
  hasMoreData = true;
  await fetchMoreDeals();
  renderPage();
});

// ページ切り替え
prevBtn.addEventListener("click", () => {
  if (currentPage > 1) {
    currentPage--;
    renderPage();
  }
});

nextBtn.addEventListener("click", async () => {
  const filteredDeals = excludeDLC ? dealsData.filter(d => !isDLC(d.title)) : dealsData;
  const totalPages = Math.ceil(filteredDeals.length / itemsPerPage);

  if (currentPage < totalPages) {
    currentPage++;
    renderPage();
  } else if (hasMoreData) {
    await fetchMoreDeals();
    const newFiltered = excludeDLC ? dealsData.filter(d => !isDLC(d.title)) : dealsData;
    const newTotalPages = Math.ceil(newFiltered.length / itemsPerPage);
    if (currentPage < newTotalPages) {
      currentPage++;
      renderPage();
    }
  }
});

// スクロールトップ
window.addEventListener("scroll", () => {
  scrollTopBtn.style.display = window.scrollY > 200 ? "block" : "none";
});
scrollTopBtn.addEventListener("click", () => {
  window.scrollTo({ top: 0, behavior: "smooth" });
});
