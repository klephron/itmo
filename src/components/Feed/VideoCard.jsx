// src/components/Feed/VideoCard.jsx
import React, { useState, useEffect, useRef } from 'react';
import { Card, CardMedia, CardContent, CardActions, Avatar, Typography, IconButton, Box, LinearProgress } from '@mui/material';
import { FavoriteBorder, Favorite, ChatBubbleOutline, Person } from '@mui/icons-material';
import { Link } from 'react-router';
import { api } from '../../services/api';
import { formatDate, formatNumber } from '../../utils/formatUtils';
import { useAuth } from '../../context/AuthContext'; // Import if needed for like/comment actions

export default function VideoCard({ video, isActive, onSetActive }) {
    const [isLiked, setIsLiked] = useState(video.likedByMe || false);
    const [likeCount, setLikeCount] = useState(video.likesCount || 0);
    const [viewCount, setViewCount] = useState(video.viewsCount || 0);
    const [comments, setComments] = useState([]);
    const [commentsLoaded, setCommentsLoaded] = useState(false);
    const videoRef = useRef(null);
    const { token } = useAuth(); // Get token from context

    // Handle video play/pause based on active state
    useEffect(() => {
        if (videoRef.current) {
            if (isActive) {
                videoRef.current.play().catch(e => console.log("Autoplay failed:", e)); // Mute might be needed
                // Register view when video starts playing
                api.viewVideo(video.uuid)
                    .then(res => setViewCount(res.viewsCount))
                    .catch(console.error);
            } else {
                videoRef.current.pause();
            }
        }
    }, [isActive, video.uuid]);

    // Load like status on mount if authenticated
    useEffect(() => {
        if (token && video.uuid) {
            api.getLikeStatus(video.uuid)
                .then(status => {
                    setIsLiked(status.liked);
                    setLikeCount(status.likesCount);
                })
                .catch(console.error);
        }
    }, [token, video.uuid]);

    // Load comments when active
    useEffect(() => {
        if (isActive && !commentsLoaded) {
            api.getComments(video.uuid)
                .then(commentsData => {
                    setComments(commentsData);
                    setCommentsLoaded(true);
                })
                .catch(console.error);
        }
    }, [isActive, commentsLoaded, video.uuid]);

    const handleLike = async () => {
        if (!token) {
            // Handle not logged in case (e.g., redirect to login)
            console.log("User not logged in");
            return;
        }
        try {
            let result;
            if (isLiked) {
                result = await api.unlikeVideo(video.uuid);
            } else {
                result = await api.likeVideo(video.uuid);
            }
            setIsLiked(!isLiked);
            setLikeCount(result.likesCount);
        } catch (error) {
            console.error("Error toggling like:", error);
        }
    };

    const handleAddComment = (newComment) => {
        setComments(prev => [newComment, ...prev]); // Optimistically add
    };

    return (
        <Card sx={{ marginBottom: 2, maxWidth: '100%' }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <Avatar src={video.uploaderAvatarUrl} alt={video.uploaderName} sx={{ mr: 1 }}>
                        {!video.uploaderAvatarUrl && <Person />}
                    </Avatar>
                    <Box>
                        <Typography variant="subtitle2" component={Link} to={`/profile/${video.uploaderId}`} sx={{ textDecoration: 'none', color: 'inherit' }}>
                            {video.uploaderName}
                        </Typography>
                        <Typography variant="caption" color="textSecondary">{formatDate(video.uploadedAt)}</Typography>
                    </Box>
                </Box>
                <Typography variant="caption" color="textSecondary">
                    {formatNumber(viewCount)} views
                </Typography>
            </CardContent>
            <Box sx={{ position: 'relative', paddingBottom: '177.78%', height: 0 }}> {/* 9:16 Aspect Ratio */}
                <video
                    ref={videoRef}
                    src={api.getVideoUrl(video.uuid)}
                    muted // Autoplay requires muted
                    loop
                    playsInline
                    style={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        width: '100%',
                        height: '100%',
                        objectFit: 'cover',
                    }}
                />
            </Box>
            <CardContent>
                <Typography gutterBottom variant="h6" component="div">
                    {video.name}
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Typography variant="caption" color="textSecondary">
                        {likeCount} likes · {comments.length} comments
                    </Typography>
                </Box>
            </CardContent>
            <CardActions disableSpacing>
                <IconButton aria-label="add to favorites" onClick={handleLike} color={isLiked ? 'error' : 'default'}>
                    {isLiked ? <Favorite /> : <FavoriteBorder />}
                </IconButton>
                <IconButton aria-label="comment">
                    <ChatBubbleOutline />
                </IconButton>
            </CardActions>
            <CardContent>
                {/* Comments Section */}
                {/*
                <CommentList comments={comments} videoUuid={video.uuid} />
                {token && <CommentInput videoUuid={video.uuid} onAddComment={handleAddComment} />}
                {!token && <Typography variant="caption" color="textSecondary">Log in to comment.</Typography>}
                */}
            </CardContent>
        </Card>
    );
}