# LEARN WITH VELMORTH — BACKEND ARCHITECTURE
## Section 11: Complete NestJS Backend

---

## 11.1 COMPLETE BACKEND FOLDER STRUCTURE

```
velmorth-backend/
├── apps/
│   ├── api-gateway/              # Not used (Kong handles this)
│   ├── auth-service/
│   │   ├── src/
│   │   │   ├── main.ts
│   │   │   ├── app.module.ts
│   │   │   ├── auth/
│   │   │   │   ├── auth.module.ts
│   │   │   │   ├── auth.controller.ts
│   │   │   │   ├── auth.service.ts
│   │   │   │   ├── strategies/
│   │   │   │   │   ├── google.strategy.ts
│   │   │   │   │   ├── apple.strategy.ts
│   │   │   │   │   └── jwt.strategy.ts
│   │   │   │   ├── guards/
│   │   │   │   │   └── jwt-auth.guard.ts
│   │   │   │   ├── dto/
│   │   │   │   │   ├── google-login.dto.ts
│   │   │   │   │   └── refresh-token.dto.ts
│   │   │   │   └── interfaces/
│   │   │   │       └── jwt-payload.interface.ts
│   │   │   └── config/
│   │   └── test/
│   │
│   ├── user-service/
│   │   ├── src/
│   │   │   ├── main.ts
│   │   │   ├── app.module.ts
│   │   │   ├── users/
│   │   │   │   ├── users.module.ts
│   │   │   │   ├── users.controller.ts
│   │   │   │   ├── users.service.ts
│   │   │   │   ├── users.repository.ts
│   │   │   │   ├── dto/
│   │   │   │   │   ├── update-profile.dto.ts
│   │   │   │   │   └── update-preferences.dto.ts
│   │   │   │   └── entities/
│   │   │   │       ├── user-profile.entity.ts
│   │   │   │       └── learning-dna.entity.ts
│   │   │   └── learning-dna/
│   │   │       ├── learning-dna.module.ts
│   │   │       ├── learning-dna.service.ts
│   │   │       └── learning-dna.calculator.ts
│   │
│   ├── lesson-service/
│   │   ├── src/
│   │   │   ├── main.ts
│   │   │   ├── app.module.ts
│   │   │   ├── lessons/
│   │   │   │   ├── lessons.module.ts
│   │   │   │   ├── lessons.controller.ts
│   │   │   │   ├── lessons.service.ts
│   │   │   │   └── lesson-session.service.ts
│   │   │   ├── exercises/
│   │   │   │   ├── exercises.module.ts
│   │   │   │   ├── exercises.service.ts
│   │   │   │   ├── validators/
│   │   │   │   │   ├── multiple-choice.validator.ts
│   │   │   │   │   ├── translation.validator.ts
│   │   │   │   │   └── arrangement.validator.ts
│   │   │   │   └── scorers/
│   │   │   │       └── exercise.scorer.ts
│   │   │   └── content/
│   │   │       ├── content.module.ts
│   │   │       └── content.service.ts
│   │
│   ├── progress-service/
│   ├── gamification-service/
│   ├── ai-service/                # Python FastAPI (separate repo)
│   ├── community-service/
│   ├── notification-service/
│   ├── analytics-service/
│   └── payment-service/
│
├── libs/                          # Shared libraries (NestJS monorepo)
│   ├── common/
│   │   ├── src/
│   │   │   ├── decorators/
│   │   │   │   ├── current-user.decorator.ts
│   │   │   │   ├── roles.decorator.ts
│   │   │   │   └── api-key.decorator.ts
│   │   │   ├── filters/
│   │   │   │   └── global-exception.filter.ts
│   │   │   ├── guards/
│   │   │   │   └── roles.guard.ts
│   │   │   ├── interceptors/
│   │   │   │   ├── logging.interceptor.ts
│   │   │   │   ├── timeout.interceptor.ts
│   │   │   │   └── transform.interceptor.ts
│   │   │   ├── pipes/
│   │   │   │   └── validation.pipe.ts
│   │   │   ├── utils/
│   │   │   │   ├── pagination.ts
│   │   │   │   └── response.ts
│   │   │   └── types/
│   │   │       └── api-response.ts
│   ├── database/
│   │   ├── src/
│   │   │   ├── database.module.ts   # TypeORM/Drizzle setup
│   │   │   └── base.repository.ts
│   ├── cache/
│   │   └── src/
│   │       ├── cache.module.ts      # Redis module
│   │       └── cache.service.ts
│   ├── events/
│   │   └── src/
│   │       ├── events.module.ts     # Kafka module
│   │       └── events.service.ts
│   └── security/
│       └── src/
│           ├── rate-limiter.ts
│           └── encryption.ts
│
├── docker/
│   ├── docker-compose.yml          # Local development
│   ├── docker-compose.test.yml     # Test environment
│   └── services/
│       ├── postgres/
│       │   └── init.sql
│       ├── redis/
│       │   └── redis.conf
│       └── kafka/
│           └── kafka-topics.sh
│
├── k8s/
│   ├── base/
│   │   ├── namespace.yaml
│   │   ├── configmap.yaml
│   │   └── secret.yaml
│   ├── auth-service/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── hpa.yaml
│   │   └── pdb.yaml
│   ├── lesson-service/
│   │   └── ...
│   └── overlays/
│       ├── development/
│       ├── staging/
│       └── production/
│
├── .github/
│   └── workflows/
│       ├── ci.yml
│       ├── cd-staging.yml
│       └── cd-production.yml
│
├── nx.json
├── package.json
└── tsconfig.base.json
```

---

## 11.2 CORE SERVICE IMPLEMENTATIONS

### Auth Service — Main Module
```typescript
// apps/auth-service/src/app.module.ts
import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { JwtModule } from '@nestjs/jwt';
import { ThrottlerModule } from '@nestjs/throttler';
import { CacheModule } from '@nestjs/cache-manager';
import { redisStore } from 'cache-manager-redis-yet';
import { AuthModule } from './auth/auth.module';
import { HealthModule } from './health/health.module';
import { EventEmitterModule } from '@nestjs/event-emitter';

@Module({
  imports: [
    // Config
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: ['.env', `.env.${process.env.NODE_ENV}`],
    }),
    
    // Database (TypeORM + PostgreSQL)
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        type: 'postgres',
        url: config.get<string>('DATABASE_URL'),
        ssl: config.get('NODE_ENV') === 'production' ? { rejectUnauthorized: true } : false,
        synchronize: false,           // Never in production
        logging: config.get('NODE_ENV') === 'development',
        entities: [__dirname + '/**/*.entity{.ts,.js}'],
        migrations: [__dirname + '/migrations/*{.ts,.js}'],
        migrationsRun: false,         // Run manually via CLI
        poolSize: 10,
        connectTimeoutMS: 10000,
        maxQueryExecutionTime: 5000,  // Slow query logging at 5s
      }),
    }),
    
    // Redis (for token blacklisting)
    CacheModule.registerAsync({
      isGlobal: true,
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: async (config: ConfigService) => ({
        store: await redisStore({
          socket: {
            host: config.get<string>('REDIS_HOST'),
            port: config.get<number>('REDIS_PORT'),
          },
          password: config.get<string>('REDIS_PASSWORD'),
          database: 0,
          ttl: 900,               // Default TTL: 15 minutes
        }),
      }),
    }),
    
    // Rate limiting
    ThrottlerModule.forRoot([
      { name: 'login', ttl: 60000, limit: 5 },        // 5 login attempts/minute
      { name: 'refresh', ttl: 60000, limit: 30 },      // 30 refresh/minute
      { name: 'general', ttl: 60000, limit: 120 },     // 120 requests/minute
    ]),
    
    // Events
    EventEmitterModule.forRoot({
      wildcard: false,
      delimiter: '.',
      maxListeners: 20,
    }),
    
    AuthModule,
    HealthModule,
  ],
})
export class AppModule {}
```

### Auth Service — Core Service
```typescript
// apps/auth-service/src/auth/auth.service.ts
import { Injectable, UnauthorizedException, ConflictException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { EventEmitter2 } from '@nestjs/event-emitter';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { CACHE_MANAGER } from '@nestjs/cache-manager';
import { Inject } from '@nestjs/common';
import { Cache } from 'cache-manager';
import { v4 as uuidv4 } from 'uuid';
import { OAuth2Client } from 'google-auth-library';
import * as bcrypt from 'bcrypt';
import { User } from './entities/user.entity';
import { Session } from './entities/session.entity';
import { OAuthAccount } from './entities/oauth-account.entity';
import { GoogleLoginDto } from './dto/google-login.dto';
import { AuthResponse } from './interfaces/auth-response.interface';

@Injectable()
export class AuthService {
  private readonly googleClient: OAuth2Client;
  private readonly JWT_ACCESS_EXPIRES = '15m';
  private readonly JWT_REFRESH_EXPIRES = '30d';
  private readonly BCRYPT_ROUNDS = 12;

  constructor(
    @InjectRepository(User)
    private readonly userRepo: Repository<User>,
    @InjectRepository(Session)
    private readonly sessionRepo: Repository<Session>,
    @InjectRepository(OAuthAccount)
    private readonly oauthRepo: Repository<OAuthAccount>,
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
    private readonly eventEmitter: EventEmitter2,
    @Inject(CACHE_MANAGER) private readonly cache: Cache,
  ) {
    this.googleClient = new OAuth2Client(
      this.configService.get<string>('GOOGLE_CLIENT_ID'),
    );
  }

  async loginWithGoogle(dto: GoogleLoginDto): Promise<AuthResponse> {
    // 1. Verify Google ID token
    const ticket = await this.googleClient.verifyIdToken({
      idToken: dto.id_token,
      audience: this.configService.get<string>('GOOGLE_CLIENT_ID'),
    });
    
    const googlePayload = ticket.getPayload();
    if (!googlePayload) throw new UnauthorizedException('Invalid Google token');
    
    const { sub: googleUserId, email, name, picture } = googlePayload;

    // 2. Find or create user
    let user: User;
    let isNewUser = false;
    
    const existingOAuth = await this.oauthRepo.findOne({
      where: { provider: 'google', providerUserId: googleUserId },
      relations: ['user'],
    });
    
    if (existingOAuth) {
      user = existingOAuth.user;
      if (!user.isActive) throw new UnauthorizedException('Account is deactivated');
    } else {
      // Check if email already exists (different provider)
      const existingUser = email ? await this.userRepo.findOne({ where: { email } }) : null;
      
      if (existingUser) {
        // Link Google to existing account
        user = existingUser;
        await this.oauthRepo.save({
          user,
          provider: 'google',
          providerUserId: googleUserId,
          providerEmail: email,
        });
      } else {
        // Create new user
        user = await this.createNewUser({
          email,
          displayName: name || email?.split('@')[0] || 'Language Learner',
          avatarUrl: picture,
          googleUserId,
        });
        isNewUser = true;
      }
    }
    
    // 3. Generate tokens
    const { accessToken, refreshToken } = await this.generateTokenPair(user);
    
    // 4. Create session
    await this.createSession(user.id, refreshToken, dto.deviceId, dto.devicePlatform, dto.deviceName);
    
    // 5. Emit event
    this.eventEmitter.emit('auth.login.success', {
      userId: user.id,
      isNewUser,
      provider: 'google',
      timestamp: new Date(),
    });
    
    // 6. Update last login
    await this.userRepo.update(user.id, {
      lastLoginAt: new Date(),
      failedLoginAttempts: 0,
    });
    
    return {
      access_token: accessToken,
      refresh_token: refreshToken,
      token_type: 'Bearer',
      expires_in: 900,
      user: this.formatUserResponse(user),
      is_new_user: isNewUser,
    };
  }

  async refreshTokens(refreshToken: string): Promise<{ access_token: string; expires_in: number }> {
    // 1. Find session
    const session = await this.sessionRepo.findOne({
      where: { refreshToken: await this.hashToken(refreshToken), isValid: true },
      relations: ['user'],
    });
    
    if (!session || session.expiresAt < new Date()) {
      throw new UnauthorizedException('Session expired or invalid');
    }
    
    if (!session.user.isActive) {
      throw new UnauthorizedException('Account deactivated');
    }
    
    // 2. Rotate refresh token (security best practice)
    const { accessToken, refreshToken: newRefreshToken } = await this.generateTokenPair(session.user);
    
    // 3. Invalidate old session, create new
    await this.sessionRepo.update(session.id, { isValid: false });
    await this.createSession(session.user.id, newRefreshToken, session.deviceId, session.devicePlatform, session.deviceName);
    
    // Store new refresh token in response (client must update stored token)
    return {
      access_token: accessToken,
      expires_in: 900,
      // Note: In production, new refresh token sent via httpOnly cookie for web, 
      // or in response body for mobile
    };
  }

  async logout(userId: string, sessionId: string, allDevices: boolean): Promise<void> {
    if (allDevices) {
      await this.sessionRepo.update({ userId }, { isValid: false });
    } else {
      await this.sessionRepo.update(sessionId, { isValid: false });
    }
    
    // Blacklist current access token in Redis (until expiry)
    await this.cache.set(`blacklist:${sessionId}`, true, 900);
    
    this.eventEmitter.emit('auth.logout', { userId, allDevices });
  }

  private async generateTokenPair(user: User): Promise<{ accessToken: string; refreshToken: string }> {
    const payload = {
      sub: user.id,
      email: user.email,
      is_premium: user.isPremium,
      roles: ['user'],
    };
    
    const accessToken = this.jwtService.sign(payload, {
      secret: this.configService.get<string>('JWT_ACCESS_SECRET'),
      expiresIn: this.JWT_ACCESS_EXPIRES,
      algorithm: 'RS256',
    });
    
    const refreshToken = uuidv4();  // Opaque token stored hashed in DB
    
    return { accessToken, refreshToken };
  }

  private async createSession(
    userId: string,
    refreshToken: string,
    deviceId?: string,
    devicePlatform?: string,
    deviceName?: string,
  ): Promise<void> {
    // Limit to 5 active sessions per user (remove oldest if exceeded)
    const activeSessions = await this.sessionRepo.count({ where: { userId, isValid: true } });
    if (activeSessions >= 5) {
      const oldest = await this.sessionRepo.findOne({
        where: { userId, isValid: true },
        order: { lastUsedAt: 'ASC' },
      });
      if (oldest) await this.sessionRepo.update(oldest.id, { isValid: false });
    }
    
    await this.sessionRepo.save({
      userId,
      refreshToken: await this.hashToken(refreshToken),
      deviceId,
      devicePlatform,
      deviceName,
      expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30 days
    });
  }

  private async hashToken(token: string): Promise<string> {
    // SHA-256 hash for secure storage (not bcrypt — tokens are random, no need for slow hash)
    const crypto = await import('crypto');
    return crypto.createHash('sha256').update(token).digest('hex');
  }

  private async createNewUser(data: {
    email?: string;
    displayName: string;
    avatarUrl?: string;
    googleUserId: string;
  }): Promise<User> {
    const user = await this.userRepo.save({
      email: data.email,
      displayName: data.displayName,
      avatarUrl: data.avatarUrl,
      isPremium: false,
    });
    
    await this.oauthRepo.save({
      user,
      provider: 'google',
      providerUserId: data.googleUserId,
      providerEmail: data.email,
    });
    
    return user;
  }
}
```

---

## 11.3 LESSON SERVICE — SESSION MANAGEMENT

```typescript
// apps/lesson-service/src/lessons/lesson-session.service.ts
import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { EventEmitter2 } from '@nestjs/event-emitter';
import { CACHE_MANAGER } from '@nestjs/cache-manager';
import { Inject } from '@nestjs/common';
import { Cache } from 'cache-manager';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';
import { LessonSession } from './entities/lesson-session.entity';
import { AnswerRecord } from './entities/answer-record.entity';
import { ExerciseItem } from '../exercises/entities/exercise-item.entity';
import { ExerciseScorer } from '../exercises/scorers/exercise.scorer';
import { StartSessionDto } from './dto/start-session.dto';
import { SubmitAnswerDto } from './dto/submit-answer.dto';

@Injectable()
export class LessonSessionService {
  private readonly SESSION_CACHE_TTL = 7200; // 2 hours
  private readonly SESSION_INACTIVE_THRESHOLD = 1800; // 30 minutes

  constructor(
    @InjectRepository(LessonSession)
    private readonly sessionRepo: Repository<LessonSession>,
    @InjectRepository(AnswerRecord)
    private readonly answerRepo: Repository<AnswerRecord>,
    @InjectRepository(ExerciseItem)
    private readonly itemRepo: Repository<ExerciseItem>,
    private readonly scorer: ExerciseScorer,
    private readonly eventEmitter: EventEmitter2,
    private readonly httpService: HttpService,
    @Inject(CACHE_MANAGER) private readonly cache: Cache,
  ) {}

  async startSession(userId: string, dto: StartSessionDto): Promise<LessonSessionData> {
    // 1. Load items (from AI service if adaptive, or from DB if standard)
    let items: ExerciseItem[];
    
    if (dto.session_type === 'ai_adaptive') {
      items = await this.getAdaptiveItems(userId, dto);
    } else if (dto.lesson_id) {
      items = await this.getLessonItems(dto.lesson_id);
    } else {
      throw new BadRequestException('Either lesson_id or ai_adaptive must be specified');
    }
    
    // 2. Personalize item order (from AI recommendation engine)
    const orderedItems = await this.getOptimalItemOrder(items, userId);
    
    // 3. Create session record
    const session = await this.sessionRepo.save({
      userId,
      lessonId: dto.lesson_id,
      sessionType: dto.session_type,
      totalItems: orderedItems.length,
      heartsRemaining: 5,
      hintsRemaining: 3,
      status: 'active',
      startedAt: new Date(),
    });
    
    // 4. Cache session state (for resume and WebSocket updates)
    const sessionState = {
      sessionId: session.id,
      userId,
      items: orderedItems,
      currentIndex: 0,
      heartsRemaining: 5,
      hintsRemaining: 3,
      xpEarned: 0,
      correctCount: 0,
      answers: [],
    };
    
    await this.cache.set(
      `session:${session.id}`,
      JSON.stringify(sessionState),
      this.SESSION_CACHE_TTL,
    );
    
    // 5. Emit session started event
    this.eventEmitter.emit('lesson.session.started', {
      userId,
      sessionId: session.id,
      lessonId: dto.lesson_id,
      itemCount: orderedItems.length,
    });
    
    return this.formatSessionResponse(session, orderedItems);
  }

  async submitAnswer(
    userId: string,
    sessionId: string,
    dto: SubmitAnswerDto,
  ): Promise<AnswerResult> {
    // 1. Load session from cache
    const cached = await this.cache.get<string>(`session:${sessionId}`);
    if (!cached) throw new NotFoundException('Session expired or not found');
    
    const sessionState = JSON.parse(cached);
    if (sessionState.userId !== userId) throw new BadRequestException('Invalid session');
    
    // 2. Get the item being answered
    const item = await this.itemRepo.findOneOrFail({ where: { id: dto.item_id } });
    
    // 3. Score the answer
    const scoreResult = await this.scorer.score(item, dto.answer, dto.hint_used);
    
    // 4. Calculate SRS quality rating (0-5)
    const qualityRating = this.calculateSrsQuality(
      scoreResult.isCorrect,
      scoreResult.score,
      dto.response_time_ms,
      dto.hint_used,
    );
    
    // 5. XP calculation
    const xpEarned = this.calculateItemXp(
      scoreResult.isCorrect,
      item.difficulty,
      dto.hint_used,
      sessionState.currentMultiplier || 1.0,
    );
    
    // 6. Save answer record
    await this.answerRepo.save({
      userId,
      sessionId,
      itemId: dto.item_id,
      userAnswer: JSON.stringify(dto.answer),
      correctAnswer: JSON.stringify(scoreResult.correctAnswer),
      isCorrect: scoreResult.isCorrect,
      qualityScore: qualityRating,
      responseTimeMs: dto.response_time_ms,
      hintWasUsed: dto.hint_used,
      wasReviewed: item.isReviewItem,
    });
    
    // 7. Update session state in cache
    sessionState.answers.push({ itemId: dto.item_id, isCorrect: scoreResult.isCorrect });
    sessionState.xpEarned += xpEarned;
    if (scoreResult.isCorrect) sessionState.correctCount++;
    else sessionState.heartsRemaining = Math.max(0, sessionState.heartsRemaining - 1);
    sessionState.currentIndex++;
    
    await this.cache.set(
      `session:${sessionId}`,
      JSON.stringify(sessionState),
      this.SESSION_CACHE_TTL,
    );
    
    // 8. Emit answer event (for gamification, progress tracking)
    this.eventEmitter.emit('lesson.item.answered', {
      userId,
      sessionId,
      itemId: dto.item_id,
      isCorrect: scoreResult.isCorrect,
      qualityScore: qualityRating,
      xpEarned,
    });
    
    // 9. Check if session complete (hearts depleted = failed, all items = complete)
    const isSessionFailed = sessionState.heartsRemaining === 0;
    const isSessionComplete = sessionState.currentIndex >= sessionState.items.length || isSessionFailed;
    
    return {
      item_id: dto.item_id,
      is_correct: scoreResult.isCorrect,
      quality_score: qualityRating,
      correct_answer: scoreResult.correctAnswer,
      xp_earned: xpEarned,
      hearts_remaining: sessionState.heartsRemaining,
      explanation: scoreResult.explanation,
      session_progress: {
        items_completed: sessionState.currentIndex,
        items_total: sessionState.items.length,
        xp_earned_so_far: sessionState.xpEarned,
        accuracy_so_far: sessionState.correctCount / sessionState.currentIndex,
      },
      is_session_complete: isSessionComplete,
    };
  }

  async completeSession(userId: string, sessionId: string): Promise<SessionResult> {
    const cached = await this.cache.get<string>(`session:${sessionId}`);
    if (!cached) throw new NotFoundException('Session not found');
    
    const sessionState = JSON.parse(cached);
    
    const accuracy = sessionState.correctCount / sessionState.items.length;
    const stars = accuracy >= 1.0 ? 3 : accuracy >= 0.8 ? 2 : 1;
    const isPerfect = accuracy >= 1.0 && !sessionState.answers.some(a => !a.isCorrect);
    
    // Update session in DB
    await this.sessionRepo.update(sessionId, {
      score: accuracy,
      accuracy,
      starsEarned: stars,
      xpEarned: sessionState.xpEarned,
      correctCount: sessionState.correctCount,
      status: 'completed',
      completedAt: new Date(),
    });
    
    // Clear cache
    await this.cache.del(`session:${sessionId}`);
    
    // Emit completion event (triggers gamification, progress, notifications)
    this.eventEmitter.emit('lesson.session.completed', {
      userId,
      sessionId,
      lessonId: sessionState.lessonId,
      accuracy,
      stars,
      xpEarned: sessionState.xpEarned,
      isPerfect,
      correctCount: sessionState.correctCount,
      totalItems: sessionState.items.length,
    });
    
    return {
      session_id: sessionId,
      results: {
        score: accuracy,
        accuracy,
        stars_earned: stars,
        xp_earned: sessionState.xpEarned,
        items_total: sessionState.items.length,
        items_correct: sessionState.correctCount,
        is_perfect: isPerfect,
      },
      // Level update, streak update, achievements come from async Kafka events
      // Client should listen on WebSocket for these real-time updates
    };
  }

  private calculateSrsQuality(
    isCorrect: boolean,
    score: number,
    responseTimeMs: number,
    hintUsed: boolean,
  ): number {
    if (!isCorrect) {
      return score > 0.5 ? 2 : 1; // Close but wrong = 2, totally wrong = 1
    }
    
    if (hintUsed) return 3; // Correct with hint = 3
    
    const responseTimeSec = responseTimeMs / 1000;
    if (responseTimeSec < 3) return 5;  // Instant correct = 5 (perfect recall)
    if (responseTimeSec < 8) return 4;  // Normal correct = 4
    return 3;                           // Slow correct = 3 (knows it, barely)
  }

  private calculateItemXp(
    isCorrect: boolean,
    difficulty: number,
    hintUsed: boolean,
    multiplier: number,
  ): number {
    if (!isCorrect) return 0;
    
    const base = 3 + Math.round(difficulty * 4); // 3-7 XP per item
    const hintPenalty = hintUsed ? 0.7 : 1.0;    // Hint = 70% XP
    
    return Math.round(base * hintPenalty * multiplier);
  }
}
```

---

## 11.4 GAMIFICATION SERVICE — EVENT CONSUMER

```typescript
// apps/gamification-service/src/consumers/lesson-completed.consumer.ts
import { Controller } from '@nestjs/common';
import { EventPattern, Payload } from '@nestjs/microservices';
import { XPService } from '../xp/xp.service';
import { StreakService } from '../streaks/streak.service';
import { AchievementService } from '../achievements/achievement.service';
import { MissionService } from '../missions/mission.service';
import { LeaderboardService } from '../leaderboards/leaderboard.service';

@Controller()
export class LessonCompletedConsumer {
  constructor(
    private readonly xpService: XPService,
    private readonly streakService: StreakService,
    private readonly achievementService: AchievementService,
    private readonly missionService: MissionService,
    private readonly leaderboardService: LeaderboardService,
  ) {}

  @EventPattern('lesson.session.completed')
  async handleLessonCompleted(@Payload() event: LessonCompletedEvent): Promise<void> {
    const { userId, xpEarned, accuracy, isPerfect, stars } = event;

    // Process all gamification updates in parallel
    await Promise.allSettled([
      // 1. Award XP with multipliers
      this.xpService.awardXP({
        userId,
        amount: xpEarned,
        source: 'lesson',
        sourceId: event.sessionId,
        multipliers: await this.xpService.getActiveMultipliers(userId),
      }),
      
      // 2. Update streak
      this.streakService.processActivity(userId),
      
      // 3. Evaluate mission progress
      this.missionService.updateProgress(userId, 'lesson_completed', {
        accuracy,
        isPerfect,
        stars,
      }),
      
      // 4. Check achievement conditions
      this.achievementService.evaluateConditions(userId, 'lesson_completed', {
        accuracy,
        isPerfect,
        consecutivePerfect: await this.xpService.getConsecutivePerfect(userId),
      }),
      
      // 5. Update leaderboard
      this.leaderboardService.addXP(userId, xpEarned),
    ]);
  }
}
```

---

## 11.5 DOCKER COMPOSE (LOCAL DEVELOPMENT)

```yaml
# docker/docker-compose.yml
version: '3.9'

services:
  # PostgreSQL
  postgres:
    image: postgres:16-alpine
    container_name: velmorth-postgres
    environment:
      POSTGRES_DB: velmorth
      POSTGRES_USER: velmorth
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./services/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U velmorth"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis Cluster (single node for dev)
  redis:
    image: redis:7-alpine
    container_name: velmorth-redis
    command: redis-server --requirepass ${REDIS_PASSWORD} --maxmemory 512mb --maxmemory-policy allkeys-lru
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Apache Kafka (via Confluent)
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: velmorth-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zookeeper_data:/var/lib/zookeeper

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: velmorth-kafka
    depends_on: [zookeeper]
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
    volumes:
      - kafka_data:/var/lib/kafka/data

  # Kafka UI (dev tool)
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: velmorth-kafka-ui
    depends_on: [kafka]
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092

  # Elasticsearch
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: velmorth-elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data

  # MinIO (object storage)
  minio:
    image: minio/minio:latest
    container_name: velmorth-minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY}
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

  # Auth Service
  auth-service:
    build:
      context: ../
      dockerfile: apps/auth-service/Dockerfile
    container_name: velmorth-auth
    ports:
      - "3001:3001"
    environment:
      NODE_ENV: development
      PORT: 3001
      DATABASE_URL: postgresql://velmorth:${POSTGRES_PASSWORD}@postgres:5432/velmorth
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_ACCESS_SECRET: ${JWT_ACCESS_SECRET}
      JWT_REFRESH_SECRET: ${JWT_REFRESH_SECRET}
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      KAFKA_BROKERS: kafka:29092
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3001/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Prometheus
  prometheus:
    image: prom/prometheus:latest
    container_name: velmorth-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=15d'

  # Grafana
  grafana:
    image: grafana/grafana:latest
    container_name: velmorth-grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD}
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources

volumes:
  postgres_data:
  redis_data:
  kafka_data:
  zookeeper_data:
  elasticsearch_data:
  minio_data:
  prometheus_data:
  grafana_data:
```

---

## 11.6 KUBERNETES DEPLOYMENT MANIFESTS

```yaml
# k8s/auth-service/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: velmorth
  labels:
    app: auth-service
    version: v1
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "3001"
        prometheus.io/path: "/metrics"
    spec:
      serviceAccountName: auth-service-sa
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 2000
      containers:
        - name: auth-service
          image: gcr.io/velmorth-prod/auth-service:v1.2.3
          imagePullPolicy: Always
          ports:
            - containerPort: 3001
              name: http
          resources:
            requests:
              cpu: "100m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
          env:
            - name: NODE_ENV
              value: "production"
            - name: DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: auth-service-secrets
                  key: database-url
            - name: JWT_ACCESS_SECRET
              valueFrom:
                secretKeyRef:
                  name: auth-service-secrets
                  key: jwt-access-secret
          livenessProbe:
            httpGet:
              path: /health/live
              port: 3001
            initialDelaySeconds: 30
            periodSeconds: 30
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /health/ready
              port: 3001
            initialDelaySeconds: 10
            periodSeconds: 10
            failureThreshold: 3
          startupProbe:
            httpGet:
              path: /health/startup
              port: 3001
            failureThreshold: 30
            periodSeconds: 5
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchExpressions:
                    - key: app
                      operator: In
                      values:
                        - auth-service
                topologyKey: kubernetes.io/hostname
---
# k8s/auth-service/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: auth-service-hpa
  namespace: velmorth
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: auth-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Pods
          value: 2
          periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Pods
          value: 1
          periodSeconds: 120
---
# k8s/auth-service/pdb.yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: auth-service-pdb
  namespace: velmorth
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: auth-service
```

---

## 11.7 CI/CD PIPELINE

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [main, develop, 'feature/**']
  pull_request:
    branches: [main, develop]

env:
  NODE_VERSION: '20.x'
  DOCKER_REGISTRY: gcr.io/velmorth-prod

jobs:
  test:
    name: Test & Lint
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: velmorth_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: --health-cmd pg_isready --health-interval 10s --health-timeout 5s --health-retries 5
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Run linting
        run: npm run lint:all
      
      - name: Type checking
        run: npm run typecheck:all
      
      - name: Run unit tests
        run: npm run test:unit -- --coverage
        env:
          DATABASE_URL: postgresql://test:test@localhost:5432/velmorth_test
          REDIS_HOST: localhost
          JWT_ACCESS_SECRET: test-secret-for-ci
      
      - name: Run integration tests
        run: npm run test:integration
        env:
          DATABASE_URL: postgresql://test:test@localhost:5432/velmorth_test
          REDIS_HOST: localhost
      
      - name: Upload coverage
        uses: codecov/codecov-action@v4
        with:
          token: ${{ secrets.CODECOV_TOKEN }}

  security:
    name: Security Scan
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'
      
      - name: OWASP Dependency Check
        uses: dependency-check/Dependency-Check_Action@main
        with:
          project: 'velmorth-backend'
          path: '.'
          format: 'SARIF'

  build-and-push:
    name: Build Docker Images
    needs: [test, security]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/develop'
    
    strategy:
      matrix:
        service: [auth-service, user-service, lesson-service, progress-service, gamification-service]
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up QEMU
        uses: docker/setup-qemu-action@v3
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      
      - name: Authenticate to GCR
        uses: google-github-actions/auth@v2
        with:
          credentials_json: ${{ secrets.GCP_SA_KEY }}
      
      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          file: apps/${{ matrix.service }}/Dockerfile
          push: true
          tags: |
            ${{ env.DOCKER_REGISTRY }}/${{ matrix.service }}:${{ github.sha }}
            ${{ env.DOCKER_REGISTRY }}/${{ matrix.service }}:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max
          build-args: |
            NODE_VERSION=${{ env.NODE_VERSION }}
            BUILD_DATE=${{ github.event.head_commit.timestamp }}
            GIT_HASH=${{ github.sha }}

  deploy-staging:
    name: Deploy to Staging
    needs: build-and-push
    runs-on: ubuntu-latest
    environment: staging
    if: github.ref == 'refs/heads/develop'
    
    steps:
      - uses: actions/checkout@v4
      - name: Deploy to GKE Staging
        uses: google-github-actions/deploy-gke@v2
        with:
          cluster: velmorth-staging
          location: us-central1
          credentials: ${{ secrets.GCP_SA_KEY }}
          manifests: |
            k8s/overlays/staging/
          images: |
            auth-service=${{ env.DOCKER_REGISTRY }}/auth-service:${{ github.sha }}
            lesson-service=${{ env.DOCKER_REGISTRY }}/lesson-service:${{ github.sha }}
```
