package com.faturial.pointofsale.app;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;


public class DitaAndFufo extends Application {

    private static Circle snake;
    private long lastShotTime = 0;
    private int score = 0;
    private int ammo = 10;
    private Text scoreText;
    private Text ammoText;
    private Double ballSpeed;
    private Text ballSpeedText;
    private AnchorPane root;
    private boolean gameOver = false;
    private List<Circle> activeCannonballs = new ArrayList<>();

    public class Cannon extends AnchorPane {

        public Cannon() {

            this.setHeight(80);
            this.setWidth(80);

            Rectangle rectangle = new Rectangle();
            rectangle.setHeight(80);
            rectangle.setWidth(40);
            rectangle.setX(20);
            rectangle.setY(0);

            Rectangle rectangle2 = new Rectangle();
            rectangle2.setHeight(40);
            rectangle2.setWidth(80);
            rectangle2.setX(0);
            rectangle2.setY(40);

            this.getChildren().addAll(rectangle, rectangle2);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Dita and Fufo");
        root = new AnchorPane();
        snake = new Circle();
        snake.setRadius(60);
        snake.setFill(Color.RED);
        snake.setCenterX(100);
        snake.setCenterY(100);

        scoreText = new Text(10, 20, "Score: 0");
        ammoText = new Text(10, 40, "Ammo: " + ammo);
        ballSpeed = 5.0;
        ballSpeedText = new Text(10, 60, "Ball Speed: " + ballSpeed);
        root.getChildren().addAll(scoreText, ammoText, ballSpeedText);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        Thread t = new Thread(this::beginCobra);
        t.start();

        root.getChildren().add(snake);

        Cannon cannon = new Cannon();
        cannon.setLayoutX(400);
        cannon.setLayoutY(520);
        root.getChildren().add(cannon);

        scene.setOnKeyPressed(event -> {
            if (gameOver) return;

            switch (event.getCode()) {
                case A:
                    cannon.setLayoutX(cannon.getLayoutX() - 20);
                    break;
                case D:
                    cannon.setLayoutX(cannon.getLayoutX() + 20);
                    break;
                case SPACE:
                    long currentTime = System.currentTimeMillis();
                    if (ammo > 0 && currentTime - lastShotTime >= 500) { // 500ms = 2 shots per second
                        lastShotTime = currentTime;
                        ammo--;
                        updateTexts();

                        Circle cannonball = new Circle(10);
                        cannonball.setCenterX(cannon.getLayoutX() + 40);
                        cannonball.setCenterY(cannon.getLayoutY());
                        root.getChildren().add(cannonball);
                        activeCannonballs.add(cannonball);

                        Timeline timeline = new Timeline();
                        timeline.setCycleCount(Timeline.INDEFINITE);
                        timeline.getKeyFrames().add(
                                new KeyFrame(Duration.millis(16), e -> {
                                    cannonball.setCenterY(cannonball.getCenterY() - 2 * ballSpeed);
                                    if (cannonball.getBoundsInParent().intersects(snake.getBoundsInParent())) {
                                        timeline.stop();
                                        root.getChildren().remove(cannonball);
                                        activeCannonballs.remove(cannonball);
                                        score++;
                                        ammo += 2;
                                        ballSpeed = 0.2 + 5 * Math.random();
                                        updateTexts();
                                        updateSnake();
                                        createExplosion(cannonball.getCenterX(), cannonball.getCenterY());
                                    } else if (cannonball.getCenterY() < 0) {
                                        timeline.stop();
                                        root.getChildren().remove(cannonball);
                                        activeCannonballs.remove(cannonball);
                                    }
                                })
                        );
                        timeline.play();
                    } else if (ammo == 0 && activeCannonballs.isEmpty()) {
                        gameOver = true;
                        displayGameOver();
                    }
                    break;
                default:
                    break;
            }
        });
    }

    private void displayGameOver() {
        Text gameOverText = new Text(150, 320, "GAME OVER");
        gameOverText.setStyle("-fx-font-size: 72px; -fx-fill: red; -fx-stroke: black;");
        root.getChildren().add(gameOverText);
    }

    private void updateSnake() {
        double startRadius = 60.0;
        double minRadius = 10.0;
        double reductionPerHit = 2.5;

        double newRadius = Math.max(minRadius, startRadius - score * reductionPerHit);
        snake.setRadius(newRadius);

        double progress = (newRadius - minRadius) / (startRadius - minRadius);
        if (progress < 0) progress = 0;
        if (progress > 1) progress = 1;

        double red = progress;
        double green = 1.0 - progress;
        snake.setFill(new Color(red, green, 0, 1.0));
    }

    private void createExplosion(double x, double y) {
        Circle explosion = new Circle(x, y, 5, Color.ORANGE);
        root.getChildren().add(explosion);

        Timeline explosionTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(explosion.radiusProperty(), 5),
                        new KeyValue(explosion.opacityProperty(), 1)
                ),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(explosion.radiusProperty(), 40),
                        new KeyValue(explosion.opacityProperty(), 0)
                )
        );
        explosionTimeline.setOnFinished(event -> root.getChildren().remove(explosion));
        explosionTimeline.play();
    }

    private void updateTexts() {
        scoreText.setText("Score: " + score);
        ammoText.setText("Ammo: " + ammo);
        ballSpeedText.setText("Ball Speed: " + String.format("%.2f", ballSpeed));
    }

    public void beginCobra() {

        int speed = 10;

        while(!gameOver) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            snake.setCenterX(snake.getCenterX() + speed);

            if(speed<0) {
                speed = speed - 10;
            }
            else {
                speed = speed + 10;
            }

            if( snake.getCenterX() + snake.getRadius() >= 800 ) {
                speed = -10;
            }

            else if( snake.getCenterX() - snake.getRadius() <= 0 ) {
                speed = 10;
            }
        }
    }



}
