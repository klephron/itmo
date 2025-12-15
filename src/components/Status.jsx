export default function Status({ id, message, type = '' }) {
    if (!message) return null; // Если нет сообщения, возвращаем null

    return (
        <div id={id} className={'status' + (type ? ' ' + type : '')}>{message}</div>
    );
}