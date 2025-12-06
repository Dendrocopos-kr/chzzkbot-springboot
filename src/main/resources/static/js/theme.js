class ThemeManager {
    constructor() {
        this.theme = localStorage.getItem('theme') || 'light';

        // 이벤트 핸들러 this 바인딩
        this.handleToggleClick = this.handleToggleClick.bind(this);
        this.handleSystemThemeChange = this.handleSystemThemeChange.bind(this);

        this.initialize();
        this.attachToggleButton();
        this.setupSystemThemeListener();
    }

    initialize() {
        if (this.theme === 'dark' || (!this.theme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
            this.theme = 'dark';
        } else {
            this.theme = 'light';
        }
        this.applyTheme();
    }

    // 현재 DOM 기준으로 토글 버튼 다시 연결
    attachToggleButton() {
        const btn = document.getElementById('theme-toggle');

        // 이전 버튼에 걸어둔 리스너 제거
        if (this.themeToggleBtn && this.themeToggleBtn !== btn) {
            this.themeToggleBtn.removeEventListener('click', this.handleToggleClick);
        }

        this.themeToggleBtn = btn;

        if (this.themeToggleBtn) {
            this.themeToggleBtn.addEventListener('click', this.handleToggleClick);
        }
    }

    setupSystemThemeListener() {
        // 시스템 다크모드 감지는 한 번만 설정
        if (this.systemMediaQuery) return;

        this.systemMediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        this.systemMediaQuery.addEventListener('change', this.handleSystemThemeChange);
    }

    handleToggleClick() {
        this.theme = this.theme === 'light' ? 'dark' : 'light';
        this.applyTheme();
    }

    handleSystemThemeChange(e) {
        // 사용자가 직접 선택한 값(localStorage)에 손대지 않을 때만 반영
        if (!localStorage.getItem('theme')) {
            this.theme = e.matches ? 'dark' : 'light';
            this.applyTheme();
        }
    }

    applyTheme() {
        document.documentElement.setAttribute('data-theme', this.theme);
        localStorage.setItem('theme', this.theme);
    }

    // Turbo에서 페이지 전환될 때마다 호출해서 버튼만 다시 붙여주면 됨
    refresh() {
        this.attachToggleButton();
    }
}

// 전역에 한 번만 생성
document.addEventListener('turbo:load', () => {
    if (!window.themeManager) {
        window.themeManager = new ThemeManager();
    } else {
        // DOM이 새로 그려졌으니 버튼만 다시 찾고 이벤트 재연결
        window.themeManager.refresh();
    }
});