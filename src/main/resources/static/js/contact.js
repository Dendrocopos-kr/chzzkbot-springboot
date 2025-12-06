document.addEventListener('turbo:load', function() {
    // FAQ 아코디언 기능
    const faqItems = document.querySelectorAll('.faq-item');

    faqItems.forEach(item => {
        const question = item.querySelector('.faq-question');
        const answer   = item.querySelector('.faq-answer');

        if (!question || !answer) return;

        // 이미 초기화된 질문이면 스킵 (중복 리스너 방지)
        if (question.dataset.init === 'true') return;
        question.dataset.init = 'true';

        question.addEventListener('click', () => {
            // 현재 클릭된 항목 이외의 모든 항목 닫기
            faqItems.forEach(otherItem => {
                if (otherItem !== item) {
                    otherItem.classList.remove('active');
                    const otherAnswer = otherItem.querySelector('.faq-answer');
                    if (otherAnswer) {
                        otherAnswer.style.display = 'none';
                    }
                }
            });

            // 현재 항목 토글
            item.classList.toggle('active');
            if (item.classList.contains('active')) {
                answer.style.display = 'block';
            } else {
                answer.style.display = 'none';
            }
        });
    });

    // 이메일 복사 기능
    const emailElements = document.querySelectorAll('a[href^="mailto:"]');

    emailElements.forEach(element => {
        // 이미 초기화된 링크면 스킵
        if (element.dataset.init === 'true') return;
        element.dataset.init = 'true';

        element.addEventListener('click', function(e) {
            e.preventDefault();

            const email = this.href.replace('mailto:', '');
            navigator.clipboard.writeText(email).then(() => {
                const originalText = this.textContent;
                this.textContent = '복사됨!';

                setTimeout(() => {
                    this.textContent = originalText;
                }, 2000);
            });
        });
    });
});
