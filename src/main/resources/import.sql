INSERT INTO videos(url, name, description, width, height, duration, fileSize, thumbnailUrl, mimetype)
VALUES ('https://cdn.testnet.cere.network/254117/baebb4ignpsyaosl65fwtslqlrz6e4avi7cyufhtlnghcak44bifsvwl2m4',
        'Video 1', 'Description 1', 720, 403, 22000, 6003397, null, 'video/quicktime'),
       ('https://cdn-1.testnet.cere.network/254117/baebb4ignpsyaosl65fwtslqlrz6e4avi7cyufhtlnghcak44bifsvwl2m4',
        'Video 2', 'Description 2', 720, 403, 22000, 6003397, null, 'video/quicktime'),
       ('https://cdn-2.testnet.cere.network/254117/baebb4ignpsyaosl65fwtslqlrz6e4avi7cyufhtlnghcak44bifsvwl2m4',
        'Video 3', 'Description 3', 720, 403, 22000, 6003397, null, 'video/quicktime'),
       ('https://cdn-3.testnet.cere.network/254117/baebb4ignpsyaosl65fwtslqlrz6e4avi7cyufhtlnghcak44bifsvwl2m4',
        'Video 4', 'Description 4', 720, 403, 22000, 6003397, null, 'video/quicktime'),
       ('https://cdn-4.testnet.cere.network/254117/baebb4ignpsyaosl65fwtslqlrz6e4avi7cyufhtlnghcak44bifsvwl2m4',
        'Video 5', 'Description 5', 720, 403, 22000, 6003397, null, 'video/quicktime')
;

INSERT INTO subscriptions(id, durationInDays, description, price)
VALUES (1, 7, 'Weekly', 0.01),
       (2, 30, 'Monthly', 0.1),
       (3, 365, 'Yearly', 1)
;

INSERT INTO user_subscriptions(address, subscription_id)
VALUES ('EQC1Tdxck3nJR_N2QiQ2nETY56IPgh2pWsGKbiGF0dSoUNNu', 1)
;