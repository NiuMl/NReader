import requests

BASE_URL = 'http://localhost:5000'

def test_health():
    print('Testing health check...')
    response = requests.get(f'{BASE_URL}/api/health')
    print(f'Status: {response.status_code}')
    print(f'Response: {response.json()}')
    print()

def test_novels():
    print('Testing novels list...')
    response = requests.get(f'{BASE_URL}/api/novels')
    print(f'Status: {response.status_code}')
    novels = response.json()
    print(f'Found {len(novels)} novels')
    for novel in novels:
        print(f"  - {novel['title']} (ID: {novel['id']})")
    print()
    return novels

def test_novel_content(novel_id):
    print(f'Testing novel content for {novel_id}...')
    response = requests.get(f'{BASE_URL}/api/novel/{novel_id}')
    print(f'Status: {response.status_code}')
    if response.status_code == 200:
        data = response.json()
        print(f'Title: {data["title"]}')
        print(f'Content length: {len(data["content"])} characters')
        print(f'Preview: {data["content"][:100]}...')
    else:
        print(f'Error: {response.json()}')
    print()

if __name__ == '__main__':
    print('NReader Backend API Test')
    print('=' * 50)
    print()
    
    try:
        test_health()
        novels = test_novels()
        
        if novels:
            test_novel_content(novels[0]['id'])
        
        print('All tests completed!')
    except requests.exceptions.ConnectionError:
        print('Error: Could not connect to server. Make sure the server is running on http://localhost:5000')
    except Exception as e:
        print(f'Error: {e}')