// 로그인 정보 로딩
let currentUser = JSON.parse(localStorage.getItem("currentUser") || "{}");
let authToken = localStorage.getItem("token");

// Toss Widgets 전역
let tossWidgets = null;

// 페이지 로드될 때 Toss 위젯 초기화 + 이벤트 등록
window.addEventListener("DOMContentLoaded", () => {

    console.log("payment.js loaded");

    // 현재 포인트 표시
    loadCurrentPoints();

    // 폼 이벤트 등록
    const form = document.getElementById("depositForm");
    if (form) {
        form.addEventListener("submit", handleDepositSubmit);
    }

    initTossWidgets();
});


// === 현재 포인트 불러오기 ===
async function loadCurrentPoints() {
    try {
        const response = await fetch("/api/point-log/me?page=0&size=1", {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${authToken}`
            }
        });

        if (!response.ok) {
            document.getElementById("currentPoints").textContent = "0P";
            return;
        }

        const apiResponse = await response.json();
        if (apiResponse.data.content.length > 0) {
            const latestLog = apiResponse.data.content[0];
            const balance = latestLog.account.balance;
            document.getElementById("currentPoints").textContent = `${balance}P`;
        } else {
            document.getElementById("currentPoints").textContent = "0P";
        }

    } catch (error) {
        console.error("포인트 불러오기 오류:", error);
        document.getElementById("currentPoints").textContent = "0P";
    }
}


// Toss 위젯 초기화
async function initTossWidgets() {
    try {
        console.log("Toss Widgets 초기화 시작");

        const clientKey = "test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm";
        const toss = TossPayments(clientKey);

        const customerKey = `user-${currentUser?.id || "guest"}`;

        tossWidgets = toss.widgets({ customerKey });

        await tossWidgets.setAmount({
            currency: "KRW",
            value: 1000,
        });

        await tossWidgets.renderPaymentMethods({
            selector: "#toss-payment-method",
            variantKey: "DEFAULT"
        });

        await tossWidgets.renderAgreement({
            selector: "#toss-agreement",
            variantKey: "AGREEMENT"
        });

        console.log("Toss Widgets 초기화 완료");

    } catch (err) {
        console.error("Toss 위젯 로딩 실패:", err);
        showAlert("결제 모듈 로딩 실패!", "error");
    }
}


// 금액 입력 후 결제 요청
async function handleDepositSubmit(event) {
    event.preventDefault();

    const amount = Number(document.getElementById("depositAmount").value);

    if (!amount || amount < 1000) {
        showAlert("최소 1,000P 이상 입력하세요", "error");
        return;
    }

    await startDepositPayment(amount);
}


// READY → Toss UI 실행
async function startDepositPayment(amount) {
    try {
        const readyRes = await fetch("/api/payments/ready", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${authToken}`
            },
            body: JSON.stringify({
                amount,
                orderId: "ORDER-" + Date.now()
            })
        });

        if (!readyRes.ok) {
            showAlert("결제 준비 실패", "error");
            return;
        }

        const readyData = await readyRes.json();
        const orderId = readyData.data.orderId;

        // 금액 반영
        await tossWidgets.setAmount({
            currency: "KRW",
            value: Number(amount)
        });

        await tossWidgets.requestPayment({
            orderId: orderId,
            orderName: "Dentallink 포인트 충전",
            successUrl: window.location.origin + "/success.html",
            failUrl: window.location.origin + "/fail.html",
            customerEmail: currentUser.email,
            customerName: currentUser.name,
            customerMobilePhone: currentUser.phone ?? "01000000000"
        });

    } catch (err) {
        console.error("결제 요청 오류:", err);
        showMessage("결제 모듈 로딩 실패!", "error");
    }
}
