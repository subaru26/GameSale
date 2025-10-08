document.addEventListener("DOMContentLoaded", async () => {
    const container = document.getElementById("deal-container");

    try {
        const response = await fetch("/api/deals");
        const deals = await response.json();

        container.innerHTML = deals.map(d => `
            <div class="card">
                <img src="${d.image || '/default.jpg'}" alt="${d.title}">
                <h3>${d.title}</h3>
                <p class="price">
                    <span class="cut">-${d.cut}%</span><br>
                    ¥${d.priceNew.toFixed(2)} <s>¥${d.priceOld.toFixed(2)}</s>
                </p>
                <p>${d.shop}</p>
                <a href="${d.url}" target="_blank">ストアで見る</a>
            </div>
        `).join("");
    } catch (e) {
        container.innerHTML = `<p>データの取得に失敗しました。</p>`;
        console.error(e);
    }
});
