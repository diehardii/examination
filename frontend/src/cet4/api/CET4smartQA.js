import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:5000/api/cet4/smart-qa',
  withCredentials: false,
  headers: {
    'Content-Type': 'application/json'
  }
});

export default {
  async fetchBackground(userId) {
    const response = await api.get('/background', {
      params: { user_id: userId }
    });
    return response.data;
  },

  askQuestionStream(userId, question, onChunk, onComplete, onError, segmentId = 'analysis') {
    const url = 'http://localhost:5000/api/cet4/smart-qa/ask-stream';
    const requestBody = JSON.stringify({
      user_id: userId,
      question,
      segment_id: segmentId
    });

    fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: requestBody
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        const processStream = () => {
          reader
            .read()
            .then(({ done, value }) => {
              if (done) {
                return;
              }

              buffer += decoder.decode(value, { stream: true });
              const lines = buffer.split('\n\n');
              buffer = lines.pop();

              lines.forEach((line) => {
                if (line.startsWith('data: ')) {
                  try {
                    const jsonData = JSON.parse(line.substring(6));
                    if (jsonData.type === 'chunk') {
                      onChunk && onChunk(jsonData.content);
                    } else if (jsonData.type === 'done') {
                      onComplete && onComplete(jsonData);
                    } else if (jsonData.type === 'error') {
                      onError && onError(new Error(jsonData.message));
                    }
                  } catch (e) {
                    onError && onError(e);
                  }
                }
              });

              processStream();
            })
            .catch((error) => {
              onError && onError(error);
            });
        };

        processStream();
      })
      .catch((error) => {
        onError && onError(error);
      });
  },

  async endSession(userId, segmentId = 'analysis') {
    const response = await api.post('/end-session', {
      user_id: userId,
      segment_id: segmentId
    });
    return response.data;
  }
};
