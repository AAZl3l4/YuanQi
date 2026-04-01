import request from '@/utils/request'

export function getMessageList(params) {
  return request.get('/api/message/list', { params })
}

export function streamChat(chatDTO, onMessage, onComplete, onError) {
  const controller = new AbortController()
  
  fetch('/api/message/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(chatDTO),
    signal: controller.signal,
    credentials: 'include'
  })
    .then(response => {
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      
      function parseSSE(text) {
        const lines = text.split('\n')
        let eventType = ''
        let data = ''
        
        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventType = line.substring(6).trim()
          } else if (line.startsWith('data:')) {
            const dataContent = line.substring(5)
            if (data) {
              data += '\n' + dataContent
            } else {
              data = dataContent
            }
          }
        }
        
        if (eventType === 'complete' || data === 'done' || data === '[DONE]') {
          onComplete?.()
          return
        }
        
        if (eventType === 'error') {
          onError?.(data)
          return
        }
        
        if (data && eventType === 'message') {
          onMessage?.(data)
        }
      }
      
      function read() {
        reader.read().then(({ done, value }) => {
          if (done) {
            if (buffer.trim()) {
              parseSSE(buffer)
            }
            onComplete?.()
            return
          }
          
          buffer += decoder.decode(value, { stream: true })
          
          const parts = buffer.split('\n\n')
          while (parts.length > 1) {
            const event = parts.shift()
            if (event.trim()) {
              parseSSE(event)
            }
          }
          buffer = parts[0] || ''
          
          read()
        }).catch(error => {
          if (error.name !== 'AbortError') {
            onError?.(error.message)
          }
        })
      }
      
      read()
    })
    .catch(error => {
      if (error.name !== 'AbortError') {
        onError?.(error.message)
      }
    })
  
  return () => controller.abort()
}
