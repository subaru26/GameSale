let dealsData = [];
let currentPage = 1;
const itemsPerPage = 48;
let totalFetched = 0;
let hasMoreData = true;
let selectedStores = [];
let loading = false;

const container = document.getElementById("dealsContainer");
const fetchDealsBtn = document.getElementById("fetchDealsBtn");
const prevBtn = document.getElementById("prevPage");
const nextBtn = document.getElementById("nextPage");
const scrollTopBtn = document.getElementById("scrollTopBtn");

// 「検索中…」メッセージ
const loadingMessage = document.createElement("p");
loadingMessage.textContent = "検索中…";
loadingMessage.className = "text-center text-gray-600 mb-2 hidden";
fetchDealsBtn.parentNode.insertBefore(loadingMessage, fetchDealsBtn.nextSibling);

fetchDealsBtn.addEventListener("click", async () => {
  if (loading) return;
  loading = true;

  const originalText = fetchDealsBtn.textContent;
  fetchDealsBtn.textContent = "検索中…";
  fetchDealsBtn.disabled = true;
  loadingMessage.classList.remove("hidden");

  selectedStores = Array.from(document.querySelectorAll(".store-checkbox:checked"))
    .map(cb => cb.value)
    .join(",");

  if (!selectedStores) {
    alert("少なくとも1つのストアを選択してください。");
    fetchDealsBtn.textContent = originalText;
    fetchDealsBtn.disabled = false;
    loadingMessage.classList.add("hidden");
    loading = false;
    return;
  }

  dealsData = [];
  currentPage = 1;
  totalFetched = 0;
  hasMoreData = true;

  await fetchMoreDeals();
  renderPage();

  fetchDealsBtn.textContent = originalText;
  fetchDealsBtn.disabled = false;
  loadingMessage.classList.add("hidden");
  loading = false;
});

async function fetchMoreDeals() {
  if (!hasMoreData) return;

  const url = `/api/deals?stores=${selectedStores}&offset=${totalFetched}&limit=200`;
  try {
    const res = await fetch(url);
    const newDeals = await res.json();

    if (!newDeals || newDeals.length === 0) {
      hasMoreData = false;
      return;
    }

    dealsData.push(...newDeals);
    totalFetched += newDeals.length;
    if (newDeals.length < 200) hasMoreData = false;
  } catch (err) {
    console.error("データ取得エラー:", err);
    alert("データ取得中にエラーが発生しました。");
    hasMoreData = false;
  }
}

function renderPage() {
  container.innerHTML = "";
  const start = (currentPage - 1) * itemsPerPage;
  const end = start + itemsPerPage;
  const pageItems = dealsData.slice(start, end);

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

  const totalPages = Math.ceil(dealsData.length / itemsPerPage);
  prevBtn.disabled = currentPage === 1;
  nextBtn.disabled = currentPage === totalPages && !hasMoreData;

  window.scrollTo({ top: 0, behavior: "smooth" });
}

// ページ切り替え
prevBtn.addEventListener("click", () => {
  if (currentPage > 1) {
    currentPage--;
    renderPage();
  }
});

nextBtn.addEventListener("click", async () => {
  const totalPages = Math.ceil(dealsData.length / itemsPerPage);

  if (currentPage < totalPages) {
    currentPage++;
    renderPage();
  } else if (hasMoreData) {
    await fetchMoreDeals();
    if ((currentPage * itemsPerPage) < dealsData.length) {
      currentPage++;
      renderPage();
    }
  }
});

// 先頭に戻るボタン
window.addEventListener("scroll", () => {
  scrollTopBtn.style.display = window.scrollY > 200 ? "block" : "none";
});

scrollTopBtn.addEventListener("click", () => {
  window.scrollTo({ top: 0, behavior: "smooth" });
});
