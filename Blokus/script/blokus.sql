-- Création de table
-- Table qui gère le machine learning
CREATE TABLE matrices (
    id serial NOT NULL,
    matrice text,
    score text
);


-- Table qui s'occupe des scores
CREATE TABLE scores (
    pseudo text NOT NULL,
    score integer NOT NULL
);

