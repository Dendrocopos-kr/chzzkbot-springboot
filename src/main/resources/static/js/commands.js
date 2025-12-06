document.addEventListener('turbo:load', function() {
    const searchInput = document.getElementById('commandSearch');
    const tableRows = document.querySelectorAll('.commands-table tbody tr');
    const noResults = document.getElementById('noResults');

    if(!searchInput || !tableRows || !noResults) return;

    if(searchInput.dataset.init === "true") return;
    searchInput.dataset.init = "true";

    if(noResults.dataset.init === "true") return;
    noResults.dataset.init = "true";

    if(tableRows.length === 0) return;

    let sortDirection = {};

    // 검색 기능 구현
    function filterCommands() {
        const searchTerm = searchInput.value.toLowerCase();
        let hasVisibleRows = false;

        tableRows.forEach(row => {
            const command = row.querySelector('.command-tag').textContent.toLowerCase();
            const response = row.cells[1].textContent.toLowerCase();
            
            if (command.includes(searchTerm) || response.includes(searchTerm)) {
                row.style.display = '';
                hasVisibleRows = true;
            } else {
                row.style.display = 'none';
            }
        });

        noResults.style.display = hasVisibleRows ? 'none' : 'block';
    }

    // 정렬 기능 구현
    function sortTable(columnIndex) {
        const tbody = document.querySelector('.commands-table tbody');
        const rows = Array.from(tableRows);
        const headers = document.querySelectorAll('.commands-table th');
        
        // 정렬 방향 토글
        sortDirection[columnIndex] = !sortDirection[columnIndex];

        // 모든 헤더의 정렬 아이콘 초기화
        headers.forEach(header => {
            const icon = header.querySelector('.sort-icon');
            icon.className = 'fas fa-sort sort-icon';
        });

        // 현재 정렬 중인 컬럼의 아이콘 업데이트
        const currentHeader = headers[columnIndex];
        const icon = currentHeader.querySelector('.sort-icon');
        icon.className = `fas fa-sort-${sortDirection[columnIndex] ? 'up' : 'down'} sort-icon`;

        // 데이터 정렬
        rows.sort((a, b) => {
            let aValue = a.cells[columnIndex].textContent;
            let bValue = b.cells[columnIndex].textContent;

            // 쿨타임 컬럼 특별 처리
            if (columnIndex === 2) {
                aValue = parseInt(aValue.replace('ms', ''));
                bValue = parseInt(bValue.replace('ms', ''));
            }
            // 마지막 호출 시간 특별 처리
            else if (columnIndex === 3) {
                aValue = new Date(aValue).getTime();
                bValue = new Date(bValue).getTime();
            }

            if (sortDirection[columnIndex]) {
                return aValue > bValue ? 1 : -1;
            } else {
                return aValue < bValue ? 1 : -1;
            }
        });

        // 정렬된 행을 테이블에 다시 추가
        rows.forEach(row => tbody.appendChild(row));
    }

    // 클립보드 복사 기능
    function addClipboardCopy() {
        document.querySelectorAll('.command-tag').forEach(tag => {
            tag.style.cursor = 'pointer';
            tag.title = '클릭하여 복사하기';

            tag.addEventListener('click', async () => {
                const textToCopy = tag.textContent;
                
                try {
                    const tempTextArea = document.createElement('textarea');
                    tempTextArea.value = textToCopy;
                    tempTextArea.style.position = 'fixed';
                    tempTextArea.style.left = '-9999px';
                    document.body.appendChild(tempTextArea);
                    
                    tempTextArea.select();
                    document.execCommand('copy');
                    document.body.removeChild(tempTextArea);
                    
                    // 복사 성공 효과
                    const originalText = tag.textContent;
                    const originalBackground = tag.style.background;
                    
                    tag.style.background = 'var(--mint, #4CAF50)';
                    tag.textContent = '복사됨!';
                    
                    setTimeout(() => {
                        tag.style.background = originalBackground;
                        tag.textContent = originalText;
                    }, 1000);
                    
                } catch (err) {
                    console.error('클립보드 복사 실패:', err);
                }
            });
        });
    }

    // 상대 시간 업데이트 함수
    function updateRelativeTimes() {
        document.querySelectorAll('.commands-table tbody tr').forEach(row => {
            const timeCell = row.cells[3]; // 마지막 호출 열
            const timestamp = new Date(timeCell.getAttribute('data-time'));
            const relativeTime = getRelativeTimeString(timestamp);
            timeCell.textContent = `${timeCell.getAttribute('data-formatted-time')} (${relativeTime})`;
        });
    }

    function getRelativeTimeString(date) {
        const now = new Date();
        const diffInSeconds = Math.floor((now - date) / 1000);

        if (diffInSeconds < 60) return '방금 전';
        if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}분 전`;
        if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}시간 전`;
        if (diffInSeconds < 2592000) return `${Math.floor(diffInSeconds / 86400)}일 전`;
        return `${Math.floor(diffInSeconds / 2592000)}개월 전`;
    }

    // 이벤트 리스너 등록
    searchInput.addEventListener('input', filterCommands);
    
    document.querySelectorAll('.commands-table th').forEach((header, index) => {
        header.addEventListener('click', () => {
            sortTable(index);
        });
    });

    // 초기화 함수 호출
    addClipboardCopy();
    updateRelativeTimes();
    
    // 1분마다 상대 시간 갱신
    setInterval(updateRelativeTimes, 60000);
});